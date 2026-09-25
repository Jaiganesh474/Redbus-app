# 🚌 redBus Multi-Agent CrewAI Journey & Travel Planner Pipeline

An automated multi-agent CI/CD and cron notification pipeline built using [CrewAI](https://crewai.com) and Google Gemini to automatically send travelers timely journey reminders, boarding warnings, and personalized dropping location travel plans.

---

## 🤖 Multi-Agent Architecture

```mermaid
graph TD
    A[Trigger: CI/CD Cron / Booking Event] --> B[Journey Auditor Agent]
    B -->|Verified Itinerary & PNR| C[Destination & Dropping Planner Agent]
    C -->|Transit Intel & Local Weather Tips| D[Communication Dispatcher Agent]
    D -->|Responsive Email| E[📧 Passenger Email (SMTP/Brevo)]
    D -->|SMS / WhatsApp Alert| F[📱 Passenger Mobile Alert]
```

### Agents:
1. **Journey Auditor Agent**:
   - Fetches live booking details (`/api/bookings/{pnr}`).
   - Extracts boarding window, bus operator details, seat assignments, and dropping point.
2. **Destination & Dropping Planner Agent**:
   - Evaluates the dropping point (e.g. Madiwala, Majestic, Koyambedu, Silk Board).
   - Generates last-mile transit suggestions (nearest metro station, pre-paid auto bays vs ride-hailing points).
   - Provides local weather forecast, early morning arrival safety advice, and nearby breakfast/refreshment hubs.
3. **Communication Dispatcher Agent**:
   - Renders a responsive HTML redBus-branded trip card email.
   - Formats and dispatches instant SMS / WhatsApp itinerary summaries.

---

## 🚀 Running Locally

1. **Navigate to the directory**:
   ```bash
   cd automation/crewai_pipeline
   ```

2. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Configure Environment Variables**:
   Copy `.env.example` to `.env` and fill in your keys:
   ```bash
   cp .env.example .env
   ```

4. **Run for a specific PNR**:
   ```bash
   python main.py --pnr RB78945612
   ```

---

## ⚙️ GitHub Actions CI/CD Integration

The workflow is configured in [`.github/workflows/crewai_journey_reminders.yml`](../../.github/workflows/crewai_journey_reminders.yml).

### Required GitHub Repository Secrets:
Go to your GitHub Repository -> **Settings** -> **Secrets and variables** -> **Actions** and add:
- `GEMINI_API_KEY`: Google AI Studio API key
- `SMTP_USERNAME`: Brevo / SMTP login username
- `SMTP_PASSWORD`: Brevo / SMTP password
- `BACKEND_API_BASE_URL`: (Optional) Production backend URL
