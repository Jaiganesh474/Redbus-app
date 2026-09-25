import 'bus_model.dart';
import 'seat_model.dart';

enum BookingStatus {
  pending,
  confirmed,
  cancelled,
  completed,
}

class PassengerInfo {
  final String name;
  final int age;
  final String gender;
  final String seatNumber;
  final double seatPrice;

  PassengerInfo({
    required this.name,
    required this.age,
    required this.gender,
    required this.seatNumber,
    required this.seatPrice,
  });

  factory PassengerInfo.fromJson(Map<String, dynamic> json) {
    return PassengerInfo(
      name: json['name'] ?? '',
      age: json['age'] ?? 25,
      gender: json['gender'] ?? 'Male',
      seatNumber: json['seatNumber'] ?? '',
      seatPrice: (json['seatPrice'] ?? json['price'] ?? 0).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'age': age,
      'gender': gender,
      'seatNumber': seatNumber,
      'seatPrice': seatPrice,
    };
  }
}

class Booking {
  final String id;
  final String pnr;
  final String tripInstanceId;
  final String busId;
  final String operatorName;
  final String busType;
  final String busNumber;
  final String sourceCity;
  final String destinationCity;
  final String travelDate;
  final String departureTime;
  final String arrivalTime;
  final BoardingDroppingPoint boardingPoint;
  final BoardingDroppingPoint droppingPoint;
  final List<PassengerInfo> passengers;
  final List<Seat> selectedSeats;
  final double baseFare;
  final double taxAndGst;
  final double discountAmount;
  final double insuranceFee;
  final double totalAmount;
  final String paymentId;
  final String paymentMethod; // 'UPI', 'CARD', 'NETBANKING', 'WALLET'
  final BookingStatus status;
  final DateTime createdAt;
  final String qrData;

  Booking({
    required this.id,
    required this.pnr,
    required this.tripInstanceId,
    required this.busId,
    required this.operatorName,
    required this.busType,
    required this.busNumber,
    required this.sourceCity,
    required this.destinationCity,
    required this.travelDate,
    required this.departureTime,
    required this.arrivalTime,
    required this.boardingPoint,
    required this.droppingPoint,
    required this.passengers,
    this.selectedSeats = const [],
    required this.baseFare,
    required this.taxAndGst,
    this.discountAmount = 0.0,
    this.insuranceFee = 0.0,
    required this.totalAmount,
    required this.paymentId,
    this.paymentMethod = 'UPI',
    this.status = BookingStatus.confirmed,
    required this.createdAt,
    required this.qrData,
  });

  bool get isUpcoming => status == BookingStatus.confirmed;
  bool get isCancelled => status == BookingStatus.cancelled;
  bool get isCompleted => status == BookingStatus.completed;

  factory Booking.fromJson(Map<String, dynamic> json) {
    BookingStatus parseStatus(String? s) {
      switch (s?.toUpperCase()) {
        case 'CONFIRMED':
          return BookingStatus.confirmed;
        case 'CANCELLED':
          return BookingStatus.cancelled;
        case 'COMPLETED':
          return BookingStatus.completed;
        default:
          return BookingStatus.pending;
      }
    }

    return Booking(
      id: json['id']?.toString() ?? '',
      pnr: json['pnr'] ?? 'RB${DateTime.now().millisecondsSinceEpoch.toString().substring(5)}',
      tripInstanceId: json['tripInstanceId']?.toString() ?? '',
      busId: json['busId']?.toString() ?? '',
      operatorName: json['operatorName'] ?? 'Premium Line',
      busType: json['busType'] ?? 'AC Sleeper (2+1)',
      busNumber: json['busNumber'] ?? 'KA-01-E-7722',
      sourceCity: json['sourceCity'] ?? 'Source',
      destinationCity: json['destinationCity'] ?? 'Destination',
      travelDate: json['travelDate'] ?? '2026-09-22',
      departureTime: json['departureTime'] ?? '21:30',
      arrivalTime: json['arrivalTime'] ?? '06:30',
      boardingPoint: BoardingDroppingPoint.fromJson(json['boardingPoint'] ?? {}),
      droppingPoint: BoardingDroppingPoint.fromJson(json['droppingPoint'] ?? {}),
      passengers: (json['passengers'] as List?)
              ?.map((p) => PassengerInfo.fromJson(p))
              .toList() ??
          [],
      baseFare: (json['baseFare'] ?? 0).toDouble(),
      taxAndGst: (json['taxAndGst'] ?? 0).toDouble(),
      discountAmount: (json['discountAmount'] ?? 0).toDouble(),
      insuranceFee: (json['insuranceFee'] ?? 0).toDouble(),
      totalAmount: (json['totalAmount'] ?? 0).toDouble(),
      paymentId: json['paymentId'] ?? '',
      paymentMethod: json['paymentMethod'] ?? 'UPI',
      status: parseStatus(json['status']),
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt']) ?? DateTime.now()
          : DateTime.now(),
      qrData: json['qrData'] ?? 'REDBUS:PNR=${json['pnr']}',
    );
  }
}
