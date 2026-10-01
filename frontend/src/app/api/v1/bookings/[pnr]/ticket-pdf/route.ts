import { NextRequest, NextResponse } from "next/server";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ pnr: string }> }
) {
  try {
    const { pnr } = await params;
    if (!pnr) {
      return new NextResponse("PNR is required", { status: 400 });
    }

    const backendUrl =
      process.env.NEXT_PUBLIC_API_URL || "https://redbus-api.duckdns.org/api/v1";

    const targetUrl = `${backendUrl.replace(/\/$/, "")}/bookings/${encodeURIComponent(
      pnr
    )}/ticket-pdf`;

    const backendRes = await fetch(targetUrl, {
      method: "GET",
      headers: {
        Accept: "application/pdf",
      },
      cache: "no-store",
    });

    if (!backendRes.ok) {
      return new NextResponse(`Error fetching PDF: ${backendRes.statusText}`, {
        status: backendRes.status,
      });
    }

    const pdfBuffer = await backendRes.arrayBuffer();

    return new NextResponse(pdfBuffer, {
      status: 200,
      headers: {
        "Content-Type": "application/pdf",
        "Content-Disposition": `attachment; filename="redbus_ticket_${pnr}.pdf"`,
        "Content-Length": pdfBuffer.byteLength.toString(),
      },
    });
  } catch (err: any) {
    return new NextResponse(`Failed to load PDF: ${err.message}`, { status: 500 });
  }
}
