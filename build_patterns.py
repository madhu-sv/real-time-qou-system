import pandas as pd
import json

print("Starting pattern generation...")

# --- Define known brands to help with extraction ---
KNOWN_BRANDS = {
    "Fage", "Annie's", "Newman's Own", "General Mills", "Blue Diamond",
    "Bonne Maman", "Philadelphia", "Stacy's", "Kellogg's", "Horizon Organic",
    "YoBaby", "Chobani", "Udi's", "Earth's Best", "Organic Valley", "Green & Black's"
}

def extract_brand(product_name):
    product_name_lower = product_name.lower()
    for brand in KNOWN_BRANDS:
        if brand.lower() in product_name_lower:
            return brand
    return "Private Label"

# --- Load the source data using pandas ---
try:
    products_df = pd.read_csv("src/main/resources/data/products.csv")
    aisles_df = pd.read_csv("src/main/resources/data/aisles.csv")
    departments_df = pd.read_csv("src/main/resources/data/departments.csv")
    print("Successfully loaded CSV files.")
except FileNotFoundError:
    print("Error: Make sure products.csv, aisles.csv, and departments.csv are in the src/main/resources/data/ directory.")
    exit()

all_patterns = []

# --- 1. Generate patterns for DEPARTMENTS ---
for department in departments_df['department'].unique():
    # FIX: Convert pattern to lowercase
    all_patterns.append({"label": "DEPARTMENT", "pattern": department.lower()})
print(f"Generated {len(departments_df['department'].unique())} patterns for departments.")

# --- 2. Generate patterns for AISLES ---
for aisle in aisles_df['aisle'].unique():
    if aisle not in ["missing", "other"]:
        # FIX: Convert pattern to lowercase
        all_patterns.append({"label": "AISLE", "pattern": aisle.lower()})
print(f"Generated {len(aisles_df['aisle'].unique())} patterns for aisles.")

# --- 3. Generate patterns for BRANDS ---
products_df['brand'] = products_df['product_name'].apply(extract_brand)
unique_brands = products_df['brand'].unique()

for brand in unique_brands:
    # Convert pattern to lowercase
    all_patterns.append({"label": "BRAND", "pattern": brand.lower()})
print(f"Generated {len(unique_brands)} patterns for brands from product names.")

# --- 3b. Ensure all KNOWN_BRANDS are included as patterns ---
for brand in KNOWN_BRANDS:
    all_patterns.append({"label": "BRAND", "pattern": brand.lower()})
print(f"Added {len(KNOWN_BRANDS)} additional known-brand patterns.")

# --- 4. Add a few other high-priority patterns ---
all_patterns.append({"label": "DIETARY_ATTRIBUTE", "pattern": "organic"})
all_patterns.append({"label": "DIETARY_ATTRIBUTE", "pattern": "gluten-free"})
all_patterns.append({"label": "DIETARY_ATTRIBUTE", "pattern": "vegan"})

# --- Write the final list to patterns.json ---
with open('patterns.json', 'w') as f:
    json.dump(all_patterns, f, indent=2)

print(f"\nSuccessfully generated {len(all_patterns)} total patterns.")
print("The 'patterns.json' file has been updated.")