from crewai import Task
from agents import (
    journey_auditor_agent,
    destination_planner_agent,
    communication_dispatcher_agent
)

def create_journey_audit_task(pnr: str) -> Task:
    return Task(
        description=(
            f"1. Use the 'Fetch Booking Details' tool to pull complete itinerary data for PNR: '{pnr}'.\n"
            "2. Extract passenger name, email, phone, boarding point, boarding time, bus details, and dropping point.\n"
            "3. Format a structured itinerary brief for the next agents."
        ),
        expected_output="A structured JSON or Markdown brief containing all verified journey parameters.",
        agent=journey_auditor_agent
    )

def create_destination_planning_task() -> Task:
    return Task(
        description=(
            "1. Using the verified itinerary from the previous task, extract the dropping point and destination city.\n"
            "2. Run the 'Analyze Dropping Location & Generate Transit Tips' tool for that specific location and arrival time.\n"
            "3. Formulate clear, practical advice including:\n"
            "   - Last-mile transit (nearest metro, auto stand vs ride-hailing cab point)\n"
            "   - Local weather & early morning/night arrival tips\n"
            "   - Nearby fresh-up / breakfast recommendations\n"
            "   - Luggage and boarding safety checks"
        ),
        expected_output="A curated travel advice and transit guide specific to the dropping location.",
        agent=destination_planner_agent
    )

def create_dispatch_notification_task() -> Task:
    return Task(
        description=(
            "1. Take the journey details and the destination transit tips.\n"
            "2. Construct a modern, premium HTML email with redBus branding (crimson #D84E55 headers, card layout, clear timing, dropping location tips).\n"
            "3. Use the 'Send Journey Reminder Email' tool to dispatch the email to the passenger's email address.\n"
            "4. Construct a concise SMS/WhatsApp text with PNR, bus name, boarding time, and dropping point tips, then send using 'Send SMS / WhatsApp Reminder'.\n"
            "5. Return a final summary report of the delivery status."
        ),
        expected_output="Delivery report showing successful dispatch of journey reminder email and mobile message.",
        agent=communication_dispatcher_agent
    )
