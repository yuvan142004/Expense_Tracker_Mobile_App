#!/usr/bin/env python3
"""
train_anomaly_model.py
======================
Generates synthetic daily Indian spending data, labels high-spend days
as anomalous, trains a binary classifier, and exports anomaly_detector.tflite.

Requirements:
    pip install tensorflow numpy

Usage:
    python train_anomaly_model.py

Output:
    anomaly_detector.tflite  ← copy to app/src/main/assets/

Model contract
--------------
  Input  : float32[1][3]
              [0]  daily_amount  / 50 000        (clamped to [0, 1])
              [1]  day_of_week   / 6             (0 = Monday … 6 = Sunday)
              [2]  day_of_month  / 30            (1-based day ÷ 30)
  Output : float32[1][1]
              anomaly probability in [0, 1]
              ≥ 0.50  →  flag as anomalous day
"""

import numpy as np
import tensorflow as tf

# ── Reproducibility ──────────────────────────────────────────────────────────
np.random.seed(42)
tf.random.set_seed(42)

# ── Constants ─────────────────────────────────────────────────────────────────
NORM_MAX           = 50_000.0   # must match AnomalyDetector.java NORM_MAX
ANOMALY_THRESHOLD  = 0.50       # must match AnomalyDetector.java ANOMALY_THRESHOLD
# An "anomaly" is defined as a daily total ≥ ANOMALY_AMOUNT_INR
ANOMALY_AMOUNT_INR = 12_000.0


# ── Synthetic data generator ──────────────────────────────────────────────────
def generate_daily_data(num_days: int = 1460):
    """
    Generate (amount, day_of_week, day_of_month, label) for num_days.
    Normal days: lognormal spend based on weekday/weekend pattern.
    Anomalous days: injected large amounts (~5 % of days).
    Salary-day pattern: 1st of each month has moderate extra spend.
    """
    amounts, dows, doms, labels = [], [], [], []
    rng = np.random.default_rng(42)

    for day in range(num_days):
        dow = day % 7                   # 0 = Monday
        dom = (day % 30) + 1           # 1-based day of month (approx.)

        # Base amount — weekends slightly higher (leisure spend)
        if dow in (5, 6):              # Saturday / Sunday
            base = rng.lognormal(mean=7.6, sigma=0.55)   # median ≈ ₹2 000
        else:
            base = rng.lognormal(mean=7.1, sigma=0.50)   # median ≈ ₹1 200

        # Salary-day extra spend (1st of month) — moderate, NOT anomalous
        if dom == 1:
            base += rng.uniform(1_000, 4_000)

        # Festival / holiday spike on specific simulated days (~3 % extra)
        if rng.random() < 0.03:
            base += rng.uniform(3_000, 7_000)

        # ── Anomaly injection ──────────────────────────────────────────────
        is_anomaly = 0
        if rng.random() < 0.05:       # 5 % anomalous days
            base += rng.uniform(ANOMALY_AMOUNT_INR, 40_000)
            is_anomaly = 1

        base = float(np.clip(base, 0, NORM_MAX))

        amounts.append(base)
        dows.append(dow)
        doms.append(dom)
        labels.append(is_anomaly)

    return (
        np.array(amounts, dtype=np.float32),
        np.array(dows,    dtype=np.float32),
        np.array(doms,    dtype=np.float32),
        np.array(labels,  dtype=np.float32),
    )


# ── Model ─────────────────────────────────────────────────────────────────────
def build_model() -> tf.keras.Model:
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(3,)),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(8,  activation='relu'),
        tf.keras.layers.Dense(1,  activation='sigmoid'),
    ], name='anomaly_detector')
    return model


# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    print("=" * 60)
    print("  Anomaly Detector — TFLite model trainer")
    print("=" * 60)

    # 1. Data
    print("\n[1/4] Generating synthetic daily spending data …")
    amounts, dows, doms, labels = generate_daily_data(num_days=1460)

    # Normalize features
    X = np.column_stack([
        amounts / NORM_MAX,
        dows    / 6.0,
        doms    / 30.0,
    ]).astype(np.float32)
    y = labels.reshape(-1, 1)

    n_anomaly = int(labels.sum())
    print(f"      Days : {len(X)}  |  Anomalies : {n_anomaly} ({n_anomaly/len(X)*100:.1f} %)")

    # 2. Balance classes — oversample anomalies 10×
    normal_idx  = np.where(labels == 0)[0]
    anomaly_idx = np.where(labels == 1)[0]
    oversampled = np.tile(anomaly_idx, 10)
    balanced_idx = np.concatenate([normal_idx, oversampled])
    rng = np.random.default_rng(7)
    rng.shuffle(balanced_idx)
    X_bal = X[balanced_idx]
    y_bal = y[balanced_idx]
    print(f"      After oversampling: {len(X_bal)} samples")

    # 3. Train
    print("\n[2/4] Training model …")
    model = build_model()
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss='binary_crossentropy',
        metrics=['accuracy', tf.keras.metrics.AUC(name='auc')],
    )
    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor='val_loss', patience=15, restore_best_weights=True
    )
    history = model.fit(
        X_bal, y_bal,
        epochs=150,
        batch_size=64,
        validation_split=0.15,
        callbacks=[early_stop],
        verbose=0,
    )
    print(f"      Stopped after {len(history.history['loss'])} epochs.")

    # 4. Evaluate on original imbalanced set
    print("\n[3/4] Evaluating on original data …")
    results = model.evaluate(X, y, verbose=0)
    print(f"      Loss={results[0]:.4f}  Acc={results[1]*100:.1f}%  AUC={results[2]:.4f}")

    # Sanity checks
    normal_sample  = np.array([[1_500 / NORM_MAX, 2/6.0, 15/30.0]], dtype=np.float32)
    anomaly_sample = np.array([[35_000 / NORM_MAX, 5/6.0, 1/30.0]], dtype=np.float32)
    score_n = model.predict(normal_sample,  verbose=0)[0][0]
    score_a = model.predict(anomaly_sample, verbose=0)[0][0]
    print(f"      Normal  ₹1 500 → score {score_n:.3f} (expect < {ANOMALY_THRESHOLD})")
    print(f"      Anomaly ₹35 000 → score {score_a:.3f} (expect ≥ {ANOMALY_THRESHOLD})")

    # 5. Export TFLite
    print("\n[4/4] Converting to TFLite …")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_bytes = converter.convert()

    out = 'anomaly_detector.tflite'
    with open(out, 'wb') as f:
        f.write(tflite_bytes)
    print(f"      Saved: {out}  ({len(tflite_bytes):,} bytes)")

    print("\n✅  Done!")
    print(f"   Copy '{out}' → app/src/main/assets/anomaly_detector.tflite")
    print()
    print("   Java contract:")
    print(f"     NORM_MAX          = {NORM_MAX}")
    print(f"     ANOMALY_THRESHOLD = {ANOMALY_THRESHOLD}")
    print("     Input  : float[1][3] = [amount÷50000, day_of_week÷6, day_of_month÷30]")
    print("     Output : float[1][1] = anomaly probability  (≥0.50 → flag day)")


if __name__ == '__main__':
    main()
