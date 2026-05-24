# Chapter 1: Why Statistics & Probability Matter in ML/DL

> *"Before we build a model, we must understand our data."*

This chapter introduces the foundational role of ** statistics and probability ** in Machine Learning.  
Rather than just theory, the code immediately applies statistical concepts to a real-world  
banking dataset — a loan default prediction problem.

---

## 📖 What This Chapter Is About

In ML/DL, models are only as good as the data fed into them. This chapter demonstrates  
why statistical thinking must come *before* any modelling — helping us understand  
the shape, health, and reliability of our data before we ever train a model.

---

## 🗂️ Dataset Used

A small **bank customer dataset** with 10 customers and 7 columns:

| Column | Description |
|--------|-------------|
| `customer_id` | Unique customer identifier |
| `age` | Customer's age |
| `income_k` | Annual income (in thousands) |
| `loan_k` | Loan amount (in thousands) |
| `years_with_bank` | How long they've been a customer |
| `missed_payments` | Number of missed payments |
| `default` | **Target** — 1 = defaulted, 0 = did not default |

---

## 💻 Code Walkthrough

The notebook is structured as a **5-step statistical health check** on the dataset:

### ✅ Step 1 — Dataset Shape
```python
df.shape  # → 10 rows, 7 columns
```
Always the first thing to check — how big is the data and what are we working with?

### ✅ Step 2 — Missing Values Check
```python
df.isna().sum()
```
Checks every column for missing/null values. Here all columns return `0` —  
meaning the dataset is clean. In real-world data, this step often reveals problems.

### ✅ Step 3 — Target Distribution
```python
df['default'].value_counts(normalize=True)
```
Checks if the dataset is **balanced** — are there roughly equal examples of  
defaulted (1) and non-defaulted (0) customers?  
Result: **60% non-default, 40% default** — reasonably balanced.

### ✅ Step 4 — Outlier Detection (IQR Method)
```python
Q1 = df[col].quantile(0.25)
Q3 = df[col].quantile(0.75)
IQR = Q3 - Q1
outliers = df[(df[col] < Q1 - 1.5*IQR) | (df[col] > Q3 + 1.5*IQR)]
```
Uses the **Interquartile Range (IQR)** method to flag extreme values in each column.  
The IQR is the range between the 25th and 75th percentile.  
Any value beyond `1.5 × IQR` above or below is flagged as an outlier.  
Result: **0 outliers detected** across all columns.

### ✅ Step 5 — Feature Leakage Check
```python
features = [col for col in df.columns if col != 'default']
```
A critical sanity check — making sure the **target column (`default`)** is not  
accidentally included as a feature. If it were, the model would "cheat" by  
seeing the answer during training.

---

## 📦 Libraries Used

| Library | Purpose |
|---------|---------|
| `pandas` | Loading and analysing the dataset |
| `io.StringIO` | Reading raw CSV string directly into a DataFrame |

---

## 🔑 Key Concepts Covered

- What a dataset shape tells you
- Why checking for missing values is non-negotiable
- Class balance and why it matters for classification
- The IQR method for outlier detection
- What feature leakage is and why it destroys models

---

## 💡 Key Takeaway

> Statistics is the lens through which raw data becomes *understandable*.  
> Every ML pipeline must start here — before any model is ever built.
