#!/usr/bin/env python3
"""
train_classifier_model.py
=========================
Generates synthetic Indian transaction data with features
[amount, hour_of_day, day_of_week] → category (0-9, maps to categoryId 1-10),
trains a dense neural network classifier, and exports category_classifier.tflite.

Requirements:
    pip install tensorflow numpy

Usage:
    python train_classifier_model.py

Output:
    category_classifier.tflite  ← copy to app/src/main/assets/

Model contract
--------------
  Input  : float32[1][3]
              [0]  amount       / 50 000      (clamped to [0, 1])
              [1]  hour_of_day  / 23          (0 = midnight … 23 = 11 pm)
              [2]  day_of_week  / 6           (0 = Monday … 6 = Sunday)
  Output : float32[1][10]
              softmax probabilities for categories 0-9
              argmax(output) + 1  =  categoryId  (1 = Food … 10 = Other)

Category mapping:
    0=Food  1=Shopping  2=FundTransfer  3=Friend  4=Bills
    5=Transport  6=Health  7=Entertainment  8=Education  9=Other
"""

import numpy as np
import tensorflow as tf

# ── Reproducibility ──────────────────────────────────────────────────────────
np.random.seed(42)
tf.random.set_seed(42)

# ── Constants ─────────────────────────────────────────────────────────────────
NUM_CATEGORIES = 10
AMOUNT_MAX     = 50_000.0   # must match SmartCategoryClassifier.java AMOUNT_MAX

# ── Category-specific spending patterns (Indian context) ──────────────────────
#
# Each entry:   'amount': (low_INR, high_INR)
#               'hours' : list of typical transaction hours (24 h clock)
#               'days'  : list of typical days (0=Mon … 6=Sun)
#
PATTERNS = {
    0: {   # Food — breakfast / lunch / dinner / evening snacks
        'amount': (30,   2_500),
        'hours' : [7, 8, 9, 12, 13, 14, 19, 20, 21, 22],
        'days'  : [0, 1, 2, 3, 4, 5, 6],
    },
    1: {   # Shopping — malls / e-commerce / groceries
        'amount': (200,  8_000),
        'hours' : [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
        'days'  : [5, 6, 0, 1, 2, 3, 4],     # skewed toward weekends
    },
    2: {   # Fund Transfer — NEFT / IMPS / UPI bank transfer
        'amount': (1_000, 50_000),
        'hours' : [9, 10, 11, 12, 13, 14, 15, 16, 17],
        'days'  : [0, 1, 2, 3, 4],            # mostly weekdays
    },
    3: {   # Friend — splitting bills / gifting
        'amount': (100,  5_000),
        'hours' : [10, 11, 12, 13, 18, 19, 20, 21, 22, 23],
        'days'  : [5, 6, 0, 1, 2, 3, 4],
    },
    4: {   # Bills & Utilities — electricity / mobile / OTT / rent
        'amount': (200,  10_000),
        'hours' : [8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18],
        'days'  : [0, 1, 2, 3, 4],
    },
    5: {   # Transport — Uber / petrol / metro / train / flight
        'amount': (30,   3_000),
        'hours' : [6, 7, 8, 9, 10, 17, 18, 19, 20, 21, 22],
        'days'  : [0, 1, 2, 3, 4, 5, 6],
    },
    6: {   # Health — doctor / pharmacy / diagnostics
        'amount': (100,  8_000),
        'hours' : [9, 10, 11, 12, 13, 14, 15, 16, 17, 18],
        'days'  : [0, 1, 2, 3, 4, 5],
    },
    7: {   # Entertainment — cinema / events / gaming
        'amount': (100,  3_000),
        'hours' : [14, 15, 16, 17, 18, 19, 20, 21, 22],
        'days'  : [5, 6, 4, 3, 0, 1, 2],     # skewed toward weekends
    },
    8: {   # Education — tuition / courses / books
        'amount': (500,  15_000),
        'hours' : [8, 9, 10, 11, 12, 13, 14, 15, 16],
        'days'  : [0, 1, 2, 3, 4],
    },
    9: {   # Other — miscellaneous
        'amount': (50,   3_000),
        'hours' : list(range(24)),
        'days'  : [0, 1, 2, 3, 4, 5, 6],
    },
}


# ── Synthetic data generator ──────────────────────────────────────────────────
def generate_data(samples_per_category: int = 1_200):
    """
    For each category, sample transactions from its pattern with Gaussian jitter,
    then normalise the features.
    Returns X (n_samples, 3) float32, y (n_samples,) int32.
    """
    rng = np.random.default_rng(42)
    X_list, y_list = [], []

    for cat, pat in PATTERNS.items():
        lo, hi = pat['amount']
        for _ in range(samples_per_category):
            # Amount — sample uniformly within range + 10 % lognormal noise
            amount = rng.uniform(lo, hi) * rng.lognormal(0, 0.10)
            amount = float(np.clip(amount, lo * 0.5, AMOUNT_MAX))

            # Hour — pick from typical hours + small Gaussian jitter
            hour_raw = float(rng.choice(pat['hours'])) + rng.normal(0, 0.6)
            hour_raw = float(np.clip(hour_raw, 0, 23))

            # Day of week — pick from typical days (some randomness)
            if rng.random() < 0.85:
                dow = int(rng.choice(pat['days']))
            else:
                dow = int(rng.integers(0, 7))

            X_list.append([amount, hour_raw, float(dow)])
            y_list.append(cat)

    X_raw = np.array(X_list, dtype=np.float32)
    y     = np.array(y_list, dtype=np.int32)

    # ── Normalize ────────────────────────────────────────────────────────────
    X = np.column_stack([
        np.clip(X_raw[:, 0] / AMOUNT_MAX, 0.0, 1.0),
        X_raw[:, 1] / 23.0,
        X_raw[:, 2] / 6.0,
    ]).astype(np.float32)

    # Shuffle
    idx = rng.permutation(len(X))
    return X[idx], y[idx]


# ── Model ─────────────────────────────────────────────────────────────────────
def build_model() -> tf.keras.Model:
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(3,)),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.25),
        tf.keras.layers.Dense(256, activation='relu'),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.25),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.Dropout(0.15),
        tf.keras.layers.Dense(64,  activation='relu'),
        tf.keras.layers.Dense(NUM_CATEGORIES, activation='softmax'),
    ], name='category_classifier')
    return model


# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    print("=" * 60)
    print("  Category Classifier — TFLite model trainer")
    print("=" * 60)

    # 1. Data
    print("\n[1/4] Generating synthetic transaction data …")
    X, y = generate_data(samples_per_category=1_200)
    y_onehot = tf.keras.utils.to_categorical(y, num_classes=NUM_CATEGORIES)
    print(f"      Samples : {len(X)}  |  Classes : {NUM_CATEGORIES}")
    print(f"      Per-class count : {np.bincount(y).tolist()}")

    # 2. Train
    print("\n[2/4] Training model …")
    model = build_model()
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=5e-4),
        loss='categorical_crossentropy',
        metrics=['accuracy'],
    )
    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor='val_accuracy', patience=20, restore_best_weights=True, mode='max'
    )
    lr_sched = tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss', factor=0.5, patience=10, min_lr=1e-6
    )
    history = model.fit(
        X, y_onehot,
        epochs=250,
        batch_size=128,
        validation_split=0.15,
        callbacks=[early_stop, lr_sched],
        verbose=0,
    )
    print(f"      Stopped after {len(history.history['loss'])} epochs.")

    # 3. Evaluate
    print("\n[3/4] Evaluating …")
    loss, acc = model.evaluate(X, y_onehot, verbose=0)
    print(f"      Loss={loss:.4f}  Acc={acc*100:.1f}%")

    # Sanity checks — (amount, hour, day_of_week) → expected category
    tests = [
        (200,   12, 5, "Food (0)"),        # cheap lunch on Saturday
        (3_000, 15, 1, "Shopping (1)"),    # afternoon shop on Tuesday
        (20_000, 10, 2, "Transfer (2)"),   # large weekday bank transfer
        (50,    19, 0, "Transport (5)"),   # cheap evening Uber on Monday
        (1_000,  9, 3, "Bills (4)"),       # morning bill payment on Thursday
    ]
    cat_names = ["Food","Shop","Transfer","Friend","Bills",
                 "Transport","Health","Entertain","Education","Other"]
    print("      Sanity checks:")
    for amt, hr, dow, expected in tests:
        xi = np.array([[amt / AMOUNT_MAX, hr / 23.0, dow / 6.0]], dtype=np.float32)
        probs = model.predict(xi, verbose=0)[0]
        pred_idx = int(np.argmax(probs))
        print(f"        ₹{amt:>6}  {hr:02d}h  {dow}→ pred={cat_names[pred_idx]}({pred_idx})  conf={probs[pred_idx]*100:.0f}%  (expected {expected})")

    # 4. Export TFLite
    print("\n[4/4] Converting to TFLite …")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_bytes = converter.convert()

    out = 'category_classifier.tflite'
    with open(out, 'wb') as f:
        f.write(tflite_bytes)
    print(f"      Saved: {out}  ({len(tflite_bytes):,} bytes)")

    print("\n✅  Done!")
    print(f"   Copy '{out}' → app/src/main/assets/category_classifier.tflite")
    print()
    print("   Java contract:")
    print(f"     AMOUNT_MAX = {AMOUNT_MAX}")
    print("     Input  : float[1][3] = [amount÷50000, hour_of_day÷23, day_of_week÷6]")
    print("     Output : float[1][10] = softmax probs; argmax + 1 = categoryId")


if __name__ == '__main__':
    main()
