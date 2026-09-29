class Review {
  final String id;
  final String userName;
  final double rating;
  final double busRating;
  final double punctualityRating;
  final double driverRating;
  final String comment;
  final String createdAt;

  Review({
    required this.id,
    required this.userName,
    required this.rating,
    this.busRating = 5.0,
    this.punctualityRating = 5.0,
    this.driverRating = 5.0,
    required this.comment,
    required this.createdAt,
  });

  factory Review.fromJson(Map<String, dynamic> json) {
    return Review(
      id: json['id']?.toString() ?? '',
      userName: json['userName'] ?? json['user_name'] ?? 'Verified Passenger',
      rating: (json['rating'] ?? json['overallRating'] ?? 4.5).toDouble(),
      busRating: (json['busRating'] ?? 4.5).toDouble(),
      punctualityRating: (json['punctualityRating'] ?? 4.5).toDouble(),
      driverRating: (json['driverRating'] ?? 4.5).toDouble(),
      comment: json['comment'] ?? '',
      createdAt: json['createdAt']?.toString().split('T').first ?? 'Recent',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'userName': userName,
      'rating': rating,
      'busRating': busRating,
      'punctualityRating': punctualityRating,
      'driverRating': driverRating,
      'comment': comment,
      'createdAt': createdAt,
    };
  }
}
