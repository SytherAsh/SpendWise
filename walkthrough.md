# SpendWise Full System Walkthrough

Welcome to the SpendWise project! This guide will walk you through the entire architecture, explain what each component does, and show you exactly how to start and test the system from end to end.

---

## 1. High-Level Architecture

SpendWise is built with a microservice-like approach to cleanly separate concerns:

1. **The Android App (APK):** The Data Collector. It sits on your phone, silently monitoring incoming SMS and notifications from banking/payment apps. It stores these raw messages locally in a Room database and syncs them to the backend.
2. **FastAPI (Python Backend):** The Brain. It receives the raw text messages from the Android app, runs regex/machine learning pipelines to extract meaningful data (Amount, Date, Merchant, Balance), and pushes structured records to the database.
3. **PostgreSQL (Supabase):** The Storage. A cloud database that holds the structured schemas (`accounts`, `recipients`, `transactions`).
4. **Spring Boot (Java Backend):** The Server. This layer exposes clean REST APIs for the processed data, acting as the foundation for any future Web Dashboard or client app.

---

## 2. Setting Up the Database

Before running any code, you need a place to store data.

1. Create a free project on [Supabase](https://supabase.com/).
2. Go to the **SQL Editor** in your Supabase dashboard.
3. Copy the SQL schema from `README.md` (in the `Journey/SpendWise` folder) and run it. This creates your `accounts`, `recipients`, and `transactions` tables.
4. Retrieve your **Project URL** and **API Key** from the Supabase settings. Also, note down your Postgres Database connection string.

---

## 3. Starting the Python FastAPI Service

This service must be running for the Android app to sync its data.

1. Open your terminal and navigate to the backend folder:
   ```bash
   cd Desktop/Journey/SpendWise/ml_service
   ```
2. Create a virtual environment and install dependencies (if you haven't already):
   ```bash
   python -m venv venv
   venv\Scripts\activate
   pip install -r requirements.txt
   ```
3. Create a `.env` file in the `ml_service` folder and add your Supabase credentials:
   ```env
   SUPABASE_URL=your_project_url
   SUPABASE_KEY=your_api_key
   ACCOUNT_ID=some_uuid_for_default_account
   ```
4. Start the server on your local network:
   ```bash
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```
   *(Note: Binding to `0.0.0.0` is critical so your phone can reach your computer's IP over Wi-Fi).*

---

## 4. Starting the Spring Boot API

This service will read the processed data from Supabase.

1. Navigate to the Spring Boot folder:
   ```bash
   cd Desktop/Journey/SpendWise/SpendWise_Backend
   ```
2. Open `src/main/resources/application.properties` and add your Supabase PostgreSQL connection details:
   ```properties
   SPRING_DATASOURCE_URL=jdbc:postgresql://<SUPABASE_DB_HOST>:5432/postgres
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=your_db_password
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   The Spring Boot API will now be live on `http://localhost:8080`.

---

## 5. Running the Android App (APK)

Now it's time to capture some data.

1. Open the **Android Studio** project located at `AndroidStudioProjects/SpendWise`.
2. Connect your physical Android phone (recommended) or start an Emulator. Build and run the app.
3. On the app dashboard, tap **Show** under System Settings.
4. Tap **Grant SMS Permission** and **Grant Notification Access**. 
   *(Note: The app needs these to read older SMS messages and catch new ones).*
5. Find your computer's local IPv4 address (e.g., `192.168.1.x`).
6. In the app's **Backend URL** field, enter your FastAPI server address:
   `http://192.168.1.x:8000/api/data`
7. Tap **Save & Test**. The API status indicator at the top should turn **Green**.

---

## 6. The End-to-End Flow

Here is exactly what happens once everything is running:

1. **You make a transaction** via UPI (e.g., Google Pay).
2. **Your bank sends an SMS** saying "₹500 debited from HDFC...".
3. **The Android App** detects the new SMS, parses the sender, and stores it in its local SQLite (Room) database.
4. **The App sends a POST request** containing the raw SMS to your Python FastAPI (`/api/data`).
5. **FastAPI receives the payload**, runs its text-processing pipelines to extract `Amount=500`, `Type=Debit`, `Bank=HDFC`.
6. **FastAPI saves the structured transaction** to Supabase.
7. You open a Web browser and call **Spring Boot** (`GET http://localhost:8080/api/transactions`).
8. **Spring Boot returns the beautifully formatted JSON** showing your exact spending!

You are now fully set up to monitor, process, and analyze your financial transactions!
