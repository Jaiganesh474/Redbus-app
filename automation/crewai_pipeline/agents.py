import os
from crewai import Agent
from langchain_google_genai import ChatGoogleGenerativeAI
from config import GEMINI_API_KEY, GEMINI_MODEL
from tools import (
    fetch_booking_details,
    analyze_dropping_location,
    send_journey_email,
    send_sms_or_whatsapp
)

def get_llm():
    if GEMINI_API_KEY:
        return ChatGoogleGenerativeAI(
            model=GEMINI_MODEL,
            google_api_key=GEMINI_API_KEY,
            temperature=0.3
        )
    return None

llm = get_llm()

# Agent 1: Journey Auditor Agent
journey_auditor_agent = Agent(
    role="Journey & Itinerary Verification Specialist",
    goal="Retrieve and verify the passenger's upcoming trip schedule, boarding window, and dropping destination.",
    backstory=(
        "You are an expert operations specialist for redBus India. You ensure all trip data "
        "(PNR, passenger contact, boarding point address, departure time, and destination drop location) "
        "are accurate and ready for dispatch."
    ),
    tools=[fetch_booking_details],
    llm=llm,
    verbose=True
)

# Agent 2: Destination & Dropping Location Planning Agent
destination_planner_agent = Agent(
    role="Local Travel & Dropping Point Navigator",
    goal="Generate actionable travel tips, last-mile transit connectivity, weather insights, and safety instructions for the specific dropping location.",
    backstory=(
        "You are an experienced local travel concierge. You know every bus stop, metro connection, "
        "auto/cab zone, local breakfast spot, and weather quirk of every Indian city. You craft customized "
        "tips for arriving passengers based on where their bus drops them."
    ),
    tools=[analyze_dropping_location],
    llm=llm,
    verbose=True
)

# Agent 3: Communications & Dispatch Specialist
communication_dispatcher_agent = Agent(
    role="Multi-Channel Travel Notification Dispatcher",
    goal="Design stunning, responsive HTML reminder emails and concise SMS/WhatsApp alerts, then send them to the traveler.",
    backstory=(
        "You are a master communicator at redBus. You craft elegant, user-friendly HTML emails "
        "with clear trip cards, boarding guidelines, and dropping point tips. You also format short, punchy "
        "SMS/WhatsApp updates to keep travelers stress-free."
    ),
    tools=[send_journey_email, send_sms_or_whatsapp],
    llm=llm,
    verbose=True
)
