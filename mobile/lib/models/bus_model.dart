import 'seat_model.dart';

class BoardingDroppingPoint {
  final String id;
  final String name;
  final String time;
  final String landmark;
  final String? address;

  BoardingDroppingPoint({
    required this.id,
    required this.name,
    required this.time,
    required this.landmark,
    this.address,
  });

  factory BoardingDroppingPoint.fromJson(Map<String, dynamic> json) {
    return BoardingDroppingPoint(
      id: json['id']?.toString() ?? '',
      name: json['name'] ?? json['pointName'] ?? '',
      time: json['time'] ?? json['pickupTime'] ?? json['dropTime'] ?? '',
      landmark: json['landmark'] ?? '',
      address: json['address'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'time': time,
      'landmark': landmark,
      'address': address,
    };
  }
}

class BusReview {
  final String id;
  final String userName;
  final double rating;
  final String comment;
  final String date;
  final List<String> tags;

  BusReview({
    required this.id,
    required this.userName,
    required this.rating,
    required this.comment,
    required this.date,
    this.tags = const [],
  });

  factory BusReview.fromJson(Map<String, dynamic> json) {
    return BusReview(
      id: json['id']?.toString() ?? '',
      userName: json['userName'] ?? 'Verified Passenger',
      rating: (json['rating'] ?? 5.0).toDouble(),
      comment: json['comment'] ?? '',
      date: json['date'] ?? 'Recent',
      tags: List<String>.from(json['tags'] ?? []),
    );
  }
}

class CancellationPolicy {
  final String timeFrame;
  final String refundPercent;

  CancellationPolicy({
    required this.timeFrame,
    required this.refundPercent,
  });
}

class Bus {
  final String id;
  final String tripInstanceId;
  final String operatorName;
  final String busType;
  final String busNumber;
  final String departureTime;
  final String arrivalTime;
  final String duration;
  final String sourceCity;
  final String destinationCity;
  final double basePrice;
  final double rating;
  final int totalRatings;
  final int availableSeats;
  final int totalSeats;
  final bool isPrimo;
  final bool isLiveTracking;
  final List<String> amenities;
  final List<String> photos;
  final List<BoardingDroppingPoint> boardingPoints;
  final List<BoardingDroppingPoint> droppingPoints;
  final List<BusReview> reviews;
  final List<Seat> seats;
  final List<CancellationPolicy> cancellationPolicies;

  Bus({
    required this.id,
    required this.tripInstanceId,
    required this.operatorName,
    required this.busType,
    required this.busNumber,
    required this.departureTime,
    required this.arrivalTime,
    required this.duration,
    required this.sourceCity,
    required this.destinationCity,
    required this.basePrice,
    required this.rating,
    required this.totalRatings,
    required this.availableSeats,
    required this.totalSeats,
    this.isPrimo = false,
    this.isLiveTracking = true,
    this.amenities = const [],
    this.photos = const [],
    this.boardingPoints = const [],
    this.droppingPoints = const [],
    this.reviews = const [],
    this.seats = const [],
    this.cancellationPolicies = const [],
  });

  factory Bus.fromJson(Map<String, dynamic> json) {
    return Bus(
      id: json['id']?.toString() ?? json['busId']?.toString() ?? '',
      tripInstanceId: json['tripInstanceId']?.toString() ?? json['tripId']?.toString() ?? '',
      operatorName: json['operatorName'] ?? json['operator']?['name'] ?? 'Premium Express',
      busType: json['busType'] ?? 'AC Sleeper 2+1',
      busNumber: json['busNumber'] ?? 'KA-01-F-9988',
      departureTime: json['departureTime'] ?? '21:00',
      arrivalTime: json['arrivalTime'] ?? '06:00',
      duration: json['duration'] ?? '9h 00m',
      sourceCity: json['sourceCity'] ?? 'Source',
      destinationCity: json['destinationCity'] ?? 'Destination',
      basePrice: (json['basePrice'] ?? json['fare'] ?? json['price'] ?? 750).toDouble(),
      rating: (json['rating'] ?? 4.5).toDouble(),
      totalRatings: json['totalRatings'] ?? json['ratingCount'] ?? 320,
      availableSeats: json['availableSeats'] ?? 18,
      totalSeats: json['totalSeats'] ?? 36,
      isPrimo: json['isPrimo'] ?? (json['rating'] != null && (json['rating'] >= 4.4)),
      isLiveTracking: json['isLiveTracking'] ?? true,
      amenities: List<String>.from(json['amenities'] ?? ['Free Wi-Fi', 'Charging Point', 'Water Bottle', 'Blanket', 'Reading Light']),
      photos: List<String>.from(json['photos'] ?? []),
      boardingPoints: (json['boardingPoints'] as List?)
              ?.map((e) => BoardingDroppingPoint.fromJson(e))
              .toList() ??
          [],
      droppingPoints: (json['droppingPoints'] as List?)
              ?.map((e) => BoardingDroppingPoint.fromJson(e))
              .toList() ??
          [],
      reviews: (json['reviews'] as List?)
              ?.map((e) => BusReview.fromJson(e))
              .toList() ??
          [],
      seats: (json['seats'] as List?)
              ?.map((e) => Seat.fromJson(e))
              .toList() ??
          [],
    );
  }
}
