import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user_model.dart';
import 'mock_data_service.dart';
import 'api_service.dart';

class AuthService {
  static const String _tokenKey = 'redbus_auth_token';
  static const String _userKey = 'redbus_user_data';

  final ApiService _apiService = ApiService();

  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  Future<User?> getCurrentUser() async {
    final prefs = await SharedPreferences.getInstance();
    final userData = prefs.getString(_userKey);
    if (userData != null) {
      try {
        return User.fromJson(jsonDecode(userData));
      } catch (_) {}
    }
    // Default logged in demo user
    return MockDataService.getMockUser();
  }

  Future<User> login({required String emailOrPhone, required String password}) async {
    // In real mode, call POST /auth/login
    final user = MockDataService.getMockUser().copyWith(
      email: emailOrPhone.contains('@') ? emailOrPhone : 'traveler@redbus.in',
      phone: !emailOrPhone.contains('@') ? emailOrPhone : '+91 98765 43210',
    );
    await _persistSession('mock_jwt_token_${DateTime.now().millisecondsSinceEpoch}', user);
    return user;
  }

  Future<User> register({
    required String name,
    required String email,
    required String phone,
    required String password,
  }) async {
    final user = User(
      id: 'usr_${DateTime.now().millisecondsSinceEpoch}',
      name: name,
      email: email,
      phone: phone,
      walletBalance: 200.0,
      savedTravellers: [
        SavedTraveller(id: 'st_init', name: name, age: 25, gender: 'Male'),
      ],
    );
    await _persistSession('mock_jwt_token_${DateTime.now().millisecondsSinceEpoch}', user);
    return user;
  }

  Future<void> updateUser(User user) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_userKey, jsonEncode(user.toJson()));
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_userKey);
    _apiService.setAuthToken(null);
  }

  Future<void> _persistSession(String token, User user) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
    await prefs.setString(_userKey, jsonEncode(user.toJson()));
    _apiService.setAuthToken(token);
  }
}
