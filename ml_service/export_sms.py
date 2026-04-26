import csv
import os
from dotenv import load_dotenv
from app.supabase_client import supabase

# Load environment variables
load_dotenv()

def export_raw_sms_to_csv(filename="sms_export.csv"):
    """
    Fetches all records from the 'raw_sms' table in Supabase 
    and saves them to a CSV file for EDA.
    """
    print(f"📡 Fetching data from 'raw_sms' table...")
    
    # Query all records from the raw_sms table
    # Note: If you have thousands of records, you might need pagination
    try:
        response = supabase.table("raw_sms").select("*").order("timestamp_ms", desc=True).execute()
        data = response.data

        if not data:
            print("⚠️ No data found in 'raw_sms' table.")
            return

        # Get the headers from the first record
        headers = data[0].keys()

        print(f"📝 Saving {len(data)} records to {filename}...")
        
        with open(filename, mode='w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=headers)
            writer.writeheader()
            for row in data:
                writer.writerow(row)

        print(f"✅ Success! Exported to {os.path.abspath(filename)}")

    except Exception as e:
        print(f"❌ Error during export: {e}")

if __name__ == "__main__":
    export_raw_sms_to_csv()
