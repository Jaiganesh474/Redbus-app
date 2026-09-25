import os
from dotenv import load_dotenv

load_dotenv()

# AI Configuration (Gemini)
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")

# RedBus Backend Configuration
BACKEND_API_BASE_URL = os.getenv("BACKEND_API_BASE_URL", "http://localhost:8080/api")

# Email Dispatch Configuration (Brevo SMTP or custom)
SMTP_HOST = os.getenv("SMTP_HOST", "smtp-relay.brevo.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", 587))
SMTP_USERNAME = os.getenv("SMTP_USERNAME", "")
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD", "")
SENDER_EMAIL = os.getenv("SENDER_EMAIL", "reminders@redbus.example.com")
SENDER_NAME = os.getenv("SENDER_NAME", "redBus Journey Assistant")

# SMS / WhatsApp webhook or provider settings
ENABLE_SMS_SIMULATION = os.getenv("ENABLE_SMS_SIMULATION", "true").lower() == "true"
