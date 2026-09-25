import os
import sys
import argparse
from crewai import Crew, Process
from agents import (
    journey_auditor_agent,
    destination_planner_agent,
    communication_dispatcher_agent
)
from tasks import (
    create_journey_audit_task,
    create_destination_planning_task,
    create_dispatch_notification_task
)

def run_reminder_pipeline(pnr: str = "RB78945612"):
    print(f"\n🚀 [redBus CrewAI Pipeline] Starting Multi-Agent Journey Notification Workflow for PNR: {pnr}")
    print("=" * 75)

    task1 = create_journey_audit_task(pnr)
    task2 = create_destination_planning_task()
    task3 = create_dispatch_notification_task()

    redbus_crew = Crew(
        agents=[
            journey_auditor_agent,
            destination_planner_agent,
            communication_dispatcher_agent
        ],
        tasks=[task1, task2, task3],
        process=Process.sequential,
        verbose=True
    )

    result = redbus_crew.kickoff()

    print("\n" + "=" * 75)
    print("🎉 [redBus CrewAI Pipeline] Workflow Completed Successfully!")
    print("=" * 75)
    print(result)
    return result

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="redBus CrewAI Journey Reminder & Travel Plan Pipeline")
    parser.add_argument("--pnr", type=str, default="RB78945612", help="Booking PNR number")
    parser.add_argument("--cron-check", action="store_true", help="Run automated batch check for all upcoming departures")
    
    args = parser.parse_args()
    run_reminder_pipeline(pnr=args.pnr)
