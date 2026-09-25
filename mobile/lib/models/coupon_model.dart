class Coupon {
  final String code;
  final String title;
  final String description;
  final double discountPercent;
  final double maxDiscount;
  final double minFare;
  final String validTill;
  final String? badge;

  Coupon({
    required this.code,
    required this.title,
    required this.description,
    required this.discountPercent,
    required this.maxDiscount,
    required this.minFare,
    required this.validTill,
    this.badge,
  });

  double calculateDiscount(double fare) {
    if (fare < minFare) return 0;
    double discount = (fare * discountPercent) / 100;
    if (discount > maxDiscount) {
      return maxDiscount;
    }
    return discount;
  }

  factory Coupon.fromJson(Map<String, dynamic> json) {
    return Coupon(
      code: json['code'] ?? '',
      title: json['title'] ?? '',
      description: json['description'] ?? '',
      discountPercent: (json['discountPercent'] ?? json['discount'] ?? 10).toDouble(),
      maxDiscount: (json['maxDiscount'] ?? 150).toDouble(),
      minFare: (json['minFare'] ?? 300).toDouble(),
      validTill: json['validTill'] ?? '31 Dec 2026',
      badge: json['badge'],
    );
  }
}
