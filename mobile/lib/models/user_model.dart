class SavedTraveller {
  final String id;
  final String name;
  final int age;
  final String gender; // 'Male', 'Female', 'Other'

  SavedTraveller({
    required this.id,
    required this.name,
    required this.age,
    required this.gender,
  });

  factory SavedTraveller.fromJson(Map<String, dynamic> json) {
    return SavedTraveller(
      id: json['id']?.toString() ?? '',
      name: json['name'] ?? '',
      age: json['age'] ?? 25,
      gender: json['gender'] ?? 'Male',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'age': age,
      'gender': gender,
    };
  }
}

class User {
  final String id;
  final String name;
  final String email;
  final String phone;
  final String? avatarUrl;
  final String? gender;
  final String role; // 'ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN'
  final double walletBalance;
  final List<SavedTraveller> savedTravellers;

  User({
    required this.id,
    required this.name,
    required this.email,
    required this.phone,
    this.avatarUrl,
    this.gender,
    this.role = 'ROLE_USER',
    this.walletBalance = 150.0,
    this.savedTravellers = const [],
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id']?.toString() ?? '',
      name: json['name'] ?? 'RedBus Traveller',
      email: json['email'] ?? '',
      phone: json['phone'] ?? '',
      avatarUrl: json['avatarUrl'],
      gender: json['gender'],
      role: json['role'] ?? 'ROLE_USER',
      walletBalance: (json['walletBalance'] ?? 150.0).toDouble(),
      savedTravellers: (json['savedTravellers'] as List?)
              ?.map((e) => SavedTraveller.fromJson(e))
              .toList() ??
          [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'email': email,
      'phone': phone,
      'avatarUrl': avatarUrl,
      'gender': gender,
      'role': role,
      'walletBalance': walletBalance,
    };
  }

  User copyWith({
    String? id,
    String? name,
    String? email,
    String? phone,
    String? avatarUrl,
    String? gender,
    String? role,
    double? walletBalance,
    List<SavedTraveller>? savedTravellers,
  }) {
    return User(
      id: id ?? this.id,
      name: name ?? this.name,
      email: email ?? this.email,
      phone: phone ?? this.phone,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      gender: gender ?? this.gender,
      role: role ?? this.role,
      walletBalance: walletBalance ?? this.walletBalance,
      savedTravellers: savedTravellers ?? this.savedTravellers,
    );
  }
}
