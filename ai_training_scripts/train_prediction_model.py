#!/usr/bin/env python3
"""
train_prediction_model.py
=========================
Generates synthetic Indian monthly spending data across 10 categories,
trains a neural-network-based regression model (LinearRegression equivalent),
and exports it as spending_predictor.tflite.

Requirements:
    pip install tensorflow numpy

Usage:
    python train_prediction_model.py

Output:
    spending_predictor.tflite  ← copy to app/src/main/assets/

Model contract
--------------
  Input  : float32[1][30]  — last 3 months × 10 categories, each divided by 50 000
  Output : float32[1][10]  — predicted next-month totals per category × 50 000 = INR

Category index mapping (0-based in tensor, 1-based in app):
    0=Food  1=Shopping  2=FundTransfer  3=Friend  4=Bills
    5=Transport  6=Health  7=Entertainment  8=Education  9=Other
"""

import numpy as np
import tensorflow as tf

# ── Reproducibility ──────────────────────────────────────────────────────────
np.random.seed(42)
tf.random.set_seed(42)

# ── Constants ─────────────────────────────────────────────────────────────────
NUM_CATEGORIES  = 10
NUM_MONTHS_IN   = 3          # look-back window used as input
INPUT_SIZE      = NUM_MONTHS_IN * NUM_CATEGORIES   # 30
NORM_MAX        = 50_000.0   # must match SpendingPredictor.java NORM_MAX

# Monthly spending ranges in INR (low, high) per category
# Realistic figures for a typical Indian household / salaried person
CATEGORY_RANGES = [
    (2_000,  15_000),   # 0 = Food
    (1_000,  12_000),   # 1 = Shopping
    (      0, 30_000),  # 2 = Fund Transfer  (highly variable)
    (  200,   5_000),   # 3 = Friend
    (1_500,   8_000),   # 4 = Bills & Utilities
    (  500,   6_000),   # 5 = Transport
    (  200,   6_000),   # 6 = Health
    (  300,   4_000),   # 7 = Entertainment
    (  500,  10_000),   # 8 = Education
    (  200,   4_000),   # 9 = Other
]


# ── Synthetic data generator ──────────────────────────────────────────────────
def generate_monthly_data(num_months: int = 120) -> np.ndarray:
    """Return array of shape (num_months, 10) with synthetic monthly spends."""
    data = np.zeros((num_months, NUM_CATEGORIES), dtype=np.float32)

    for m in range(num_months):
        moy = m % 12  # month-of-year (0 = January)

        for c, (lo, hi) in enumerate(CATEGORY_RANGES):
            base = np.random.uniform(lo, hi)

            # ── Seasonal adjustments ────────────────────────────────────────
            if c == 0 and moy in (9, 10):    # Food ↑ Diwali / Navratri
                base *= np.random.uniform(1.2, 1.4)
            if c == 1 and moy in (9, 10):    # Shopping ↑ festival season
                base *= np.random.uniform(1.3, 1.6)
            if c == 4 and moy in (3, 4, 5):  # Bills ↑ summer (AC)
                base *= np.random.uniform(1.1, 1.3)
            if c == 8 and moy in (5, 6):     # Education ↑ new academic year
                base *= np.random.uniform(1.8, 2.5)
            if c == 5 and moy in (10, 11, 0): # Transport ↑ winter weddings
                base *= np.random.uniform(1.1, 1.2)

            # ── Gradual inflation trend (~6 % p.a.) ─────────────────────────
            base *= (1.0 + 0.06 / 12) ** m

            data[m, c] = max(0.0, base)

    return data


def build_dataset(monthly: np.ndarray):
    """Slide a 3-month window to build (X, y) pairs."""
    X, y = [], []
    for i in range(NUM_MONTHS_IN, len(monthly)):
        X.append(monthly[i - NUM_MONTHS_IN : i].flatten())
        y.append(monthly[i])
    return np.array(X, dtype=np.float32), np.array(y, dtype=np.float32)


# ── Model ─────────────────────────────────────────────────────────────────────
def build_model() -> tf.keras.Model:
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(INPUT_SIZE,)),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.15),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dropout(0.10),
        tf.keras.layers.Dense(32, activation='relu'),
        # relu on output → predictions are always non-negative
        tf.keras.layers.Dense(NUM_CATEGORIES, activation='relu'),
    ], name='spending_predictor')
    return model


# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    print("=" * 60)
    print("  Spending Predictor — TFLite model trainer")
    print("=" * 60)

    # 1. Data
    print("\n[1/4] Generating synthetic Indian spending data …")
    monthly_raw = generate_monthly_data(num_months=240)   # 20 years synthetic data
    X_raw, y_raw = build_dataset(monthly_raw)
    X = X_raw / NORM_MAX
    y = y_raw / NORM_MAX
    print(f"      Samples : {len(X)}  |  Input dim : {X.shape[1]}  |  Output dim : {y.shape[1]}")

    # 2. Train
    print("\n[2/4] Training model …")
    model = build_model()
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss='huber',        # robust to outliers (large fund transfers)
        metrics=['mae'],
    )

    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor='val_loss', patience=20, restore_best_weights=True
    )
    lr_sched = tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss', factor=0.5, patience=10, min_lr=1e-5
    )

    history = model.fit(
        X, y,
        epochs=300,
        batch_size=32,
        validation_split=0.15,
        callbacks=[early_stop, lr_sched],
        verbose=0,
    )
    epochs_run = len(history.history['loss'])
    print(f"      Stopped after {epochs_run} epochs.")

    # 3. Evaluate
    print("\n[3/4] Evaluating …")
    _, mae_norm = model.evaluate(X, y, verbose=0)
    print(f"      MAE (normalized) : {mae_norm:.5f}")
    print(f"      MAE (INR)        : ₹{mae_norm * NORM_MAX:,.0f}")

    # Quick sanity check
    sample_x = X[:3]                            # oldest 3 months
    pred = model.predict(sample_x, verbose=0)
    print(f"      Sample prediction (INR): {[f'₹{v*NORM_MAX:,.0f}' for v in pred[0]]}")

    # 4. Export TFLite
    print("\n[4/4] Converting to TFLite …")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_bytes = converter.convert()

    out = 'spending_predictor.tflite'
    with open(out, 'wb') as f:
        f.write(tflite_bytes)
    print(f"      Saved: {out}  ({len(tflite_bytes):,} bytes)")

    print("\n✅  Done!")
    print(f"   Copy '{out}' → app/src/main/assets/spending_predictor.tflite")
    print()
    print("   Java contract:")
    print(f"     NORM_MAX  = {NORM_MAX}")
    print("     Input  : float[1][30] — 3 months × 10 categories ÷ NORM_MAX")
    print("     Output : float[1][10] — multiply by NORM_MAX for INR predictions")


if __name__ == '__main__':
    main()
