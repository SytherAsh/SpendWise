import pandas as pd
import numpy as np
import os
import re

class FinancialSmsProcessor:
    """
    Handles processing of raw SMS/Notification data to extract precise
    financial details, segregate spam, and deduplicate cross-platform entries.
    """
    
    def __init__(self, input_file="captured_sms.csv", output_file="clean_sms_eda.csv", financial_output_file="true_financial_sms.csv"):
        self.input_file = input_file
        self.output_file = output_file
        self.financial_output_file = financial_output_file

    def _parse_financial_sms(self, df):
        """
        Parses the SMS body using Regex to extract precise financial details
        and accurately segregate financial from non-financial messages.
        """
        # Ensure body and sender are strings
        df['body'] = df['body'].fillna('').astype(str)
        df['sender'] = df['sender'].fillna('').astype(str)

        # 1. Base rule: identify explicit spam/ads based on common keywords
        ad_keywords = r'(?i)(?:recharge|data pack|data loan|validity|playlist|subscription|claim|ott|netflix|jiohotstar|zee5|apple music|free access|call alert|hello tune|missed call)'
        
        # 2. Amount Extraction
        amount_pattern = r'(?i)(?:Rs\.?|INR)\s*([\d,]+\.?\d*)'
        alt_amount_pattern = r'(?i)(?:debited by|credited by)\s*([\d,]+\.?\d*)'
        
        amt_match = df['body'].str.extract(amount_pattern)[0]
        alt_amt_match = df['body'].str.extract(alt_amount_pattern)[0]
        df['parsed_amount'] = amt_match.fillna(alt_amt_match)
        df['parsed_amount'] = pd.to_numeric(df['parsed_amount'].str.replace(',', '', regex=False), errors='coerce')
        
        # 3. Direction Extraction (Credit / Debit)
        debit_keywords = r'(?i)(?:debited|deducted|paid|spent|withdrawn)'
        credit_keywords = r'(?i)(?:credited|received|added|refunded)'
        
        conditions = [
            df['body'].str.contains(debit_keywords, na=False),
            df['body'].str.contains(credit_keywords, na=False)
        ]
        choices = ['DEBIT', 'CREDIT']
        df['parsed_direction'] = np.select(conditions, choices, default=None)
        
        # 4. Reference ID Extraction
        ref_pattern = r'(?i)(?:Refno|Ref No|Txn ID|Transaction ID|UPI Ref|TxnNo)\s*[:\-]?\s*([A-Za-z0-9]+)'
        df['parsed_ref_id'] = df['body'].str.extract(ref_pattern)[0]
        
        # 5. Entity Extraction (Recipient/Sender)
        entity_debit_pattern = r'(?i)(?:trf to|transfer to|transferred to)\s+(.*?)\s+(?:Refno|Ref |UPI|Txn|Avl|dt\s|\()'
        entity_credit_pattern = r'(?i)(?:transfer from|by a/c linked to)\s+(.*?)\s+(?:Ref No|Ref |UPI|Txn|\()'
        
        ent_debit = df['body'].str.extract(entity_debit_pattern)[0]
        ent_credit = df['body'].str.extract(entity_credit_pattern)[0]
        
        sender_clean = df['sender'].str.replace(r'^[A-Z]{2}-|-?[A-Z]$', '', regex=True)
        df['parsed_entity'] = ent_debit.fillna(ent_credit).fillna(sender_clean)
        
        # 6. Final Financial Labeling
        is_ad = df['body'].str.contains(ad_keywords, na=False) | df['sender'].str.contains(r'(?i)(?:AIRTEL|JIO|VI|650025|BURKIN|ZUDIO|PTRENG)', na=False)
        has_financial_info = df['parsed_amount'].notna() & df['parsed_direction'].notna() & (df['parsed_amount'] > 0)
        df['parsed_is_financial'] = has_financial_info & (~is_ad)
        
        return df

    def process_all(self):
        """
        Executes the full pipeline:
        1. Reads data
        2. Deduplicates exact matches
        3. Parses regex
        4. Cross-platform deduplication (SMS vs Notification)
        5. Saves output CSVs
        """
        if not os.path.exists(self.input_file):
            print(f"Error: {self.input_file} not found. Please sync some data first!")
            return

        print(f"Loading {self.input_file}...")
        df = pd.read_csv(self.input_file)

        # 1. Remove exact duplicates
        initial_count = len(df)
        df = df.drop_duplicates(subset=['body', 'timestamp_ms'])
        print(f"Removed {initial_count - len(df)} duplicate records.")

        # 2. Handle mixed formats in timestamp_ms (some are epoch ms, some are datetime strings)
        numeric_ms = pd.to_numeric(df['timestamp_ms'], errors='coerce')
        dt_from_ms = pd.to_datetime(numeric_ms, unit='ms', errors='coerce')
        dt_from_str = pd.to_datetime(df['timestamp_ms'], errors='coerce')
        df['parsed_datetime'] = dt_from_ms.fillna(dt_from_str)

        # 3. Filter invalid and non-2026 timestamps
        invalid_dates = df['parsed_datetime'].isna()
        if invalid_dates.any():
            print(f"Dropped {invalid_dates.sum()} records with missing/invalid timestamps.")
            df = df[~invalid_dates]

        pre_year_filter_count = len(df)
        df = df[df['parsed_datetime'].dt.year == 2026].copy()
        print(f"Dropped {pre_year_filter_count - len(df)} records not from 2026.")

        # Sort by actual time
        df = df.sort_values(by='parsed_datetime', ascending=True)

        # 4. Add date column robustly
        df['date'] = df['parsed_datetime'].dt.date.astype(str)

        # 5. Parse Financial details
        print("Parsing financial details using Regex...")
        df = self._parse_financial_sms(df)
        
        # 5. Overwrite unreliable columns
        df['is_financial'] = df['parsed_is_financial']
        df['amount'] = df['parsed_amount']
        df['direction'] = df['parsed_direction']
        df['ref_id'] = df['parsed_ref_id']
        df['entity'] = df['parsed_entity']
        

        cols_to_drop = ['parsed_is_financial', 'parsed_amount', 'parsed_direction', 'parsed_ref_id', 'parsed_entity']
        df = df.drop(columns=cols_to_drop)

        # 6. Cross-Platform Deduplication (2-minute window)
        pre_dedup_count = len(df)
        
        # Deduplicate based on amount, direction, and 2-minute time bucket
        df['time_bucket_2m'] = df['parsed_datetime'].dt.floor('2min')
        df = df.drop_duplicates(subset=['amount', 'direction', 'time_bucket_2m'])
        df = df.drop(columns=['time_bucket_2m', 'parsed_datetime'])
        
        cross_dup_count = pre_dedup_count - len(df)
        if cross_dup_count > 0:
            print(f"Removed {cross_dup_count} cross-platform duplicates (SMS + Notification overlap).")

        # 7. Save outputs
        df.to_csv(self.output_file, index=False)
        print(f"Success! Clean data saved to: {self.output_file}")
        
        financial_df = df[df['is_financial'] == True]
        financial_df.to_csv(self.financial_output_file, index=False)
        print(f"Saved strictly financial data to: {self.financial_output_file}")
        
        return {
            "total_records": len(df),
            "financial_count": len(financial_df),
            "cross_platform_duplicates_removed": cross_dup_count
        }

if __name__ == "__main__":
    processor = FinancialSmsProcessor()
    processor.process_all()
