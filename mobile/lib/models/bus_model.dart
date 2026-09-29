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

  factory BoardingDroppingPoint.fromJson(dynamic data) {
    if (data is String) {
      final parts = data.split(' ');
      final time = parts.isNotEmpty && parts.last.contains(':') ? parts.last : '';
      final name = parts.isNotEmpty && parts.last.contains(':')
          ? parts.sublist(0, parts.length - 1).join(' ')
          : data;
      return BoardingDroppingPoint(
        id: 'pt_${data.hashCode}',
        name: name.isNotEmpty ? name : data,
        time: time.isNotEmpty ? time : '06:00',
        landmark: 'Main Highway / City Center',
        address: data,
      );
    }
    if (data is Map<String, dynamic>) {
      return BoardingDroppingPoint(
        id: data['id']?.toString() ?? 'pt_${data.hashCode}',
        name: data['name'] ?? data['pointName'] ?? data['location'] ?? 'Station',
        time: data['time'] ?? data['pickupTime'] ?? data['dropTime'] ?? '06:00',
        landmark: data['landmark'] ?? 'Near Major Terminal',
        address: data['address'] ?? data['name'],
      );
    }
    return BoardingDroppingPoint(
      id: 'pt_default',
      name: 'Central Bus Terminal',
      time: '06:00',
      landmark: 'Main Junction',
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
    final rawDep = json['departureTime']?.toString() ?? '21:00';
    final rawArr = json['arrivalTime']?.toString() ?? '06:00';
    final depTime = rawDep.length >= 5 ? rawDep.substring(0, 5) : rawDep;
    final arrTime = rawArr.length >= 5 ? rawArr.substring(0, 5) : rawArr;

    String formatDuration() {
      if (json['durationHours'] != null) {
        final hoursNum = (json['durationHours'] as num).toDouble();
        final hours = hoursNum.floor();
        final minutes = ((hoursNum - hours) * 60).round();
        return '${hours}h ${minutes.toString().padLeft(2, '0')}m';
      }
      return json['duration'] ?? '8h 30m';
    }

    final rawBoarding = json['boardingPoints'] as List? ?? ['Majestic Bus Terminal 21:00', 'Electronic City Toll 21:45'];
    final rawDropping = json['droppingPoints'] as List? ?? ['Gachibowli Junction 05:30', 'Ameerpet Metro 06:15'];

    return Bus(
      id: json['id']?.toString() ?? json['routeId']?.toString() ?? json['busId']?.toString() ?? '',
      tripInstanceId: json['tripInstanceId']?.toString() ?? json['id']?.toString() ?? '',
      operatorName: json['operatorName'] ?? json['operator']?['name'] ?? 'IntrCity SmartBus',
      busType: json['busType'] ?? 'AC Sleeper (2+1)',
      busNumber: json['busNumber'] ?? 'KA-51-AB-${json['id'] ?? 4402}',
      departureTime: depTime,
      arrivalTime: arrTime,
      duration: formatDuration(),
      sourceCity: json['sourceCity'] ?? 'Source',
      destinationCity: json['destinationCity'] ?? 'Destination',
      basePrice: (json['basePrice'] ?? json['fare'] ?? json['price'] ?? 750).toDouble(),
      rating: (json['rating'] ?? 4.5).toDouble(),
      totalRatings: json['totalRatings'] ?? json['ratingCount'] ?? 428,
      availableSeats: json['availableSeats'] ?? 24,
      totalSeats: json['totalSeats'] ?? 36,
      isPrimo: json['isPrimo'] ?? ((json['rating'] ?? 4.0) >= 4.4),
      isLiveTracking: json['isLiveTracking'] ?? true,
      amenities: List<String>.from(json['amenities'] ?? [
        'Free Wi-Fi',
        'Charging Point',
        'Water Bottle',
        'Blanket & Pillow',
        'Reading Light'
      ]),
      photos: json['busPhotoUrl'] != null
          ? [json['busPhotoUrl']]
          : List<String>.from(json['photos'] ?? []),
      boardingPoints: rawBoarding.map((e) => BoardingDroppingPoint.fromJson(e)).toList(),
      droppingPoints: rawDropping.map((e) => BoardingDroppingPoint.fromJson(e)).toList(),
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
