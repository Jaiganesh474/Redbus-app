import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../config/constants.dart';
import '../models/user_model.dart';
import 'api_service.dart';

class AuthService {
  static const String _tokenKey = 'redbus_auth_token';
  static const String _userKey = 'redbus_user_data';

  final ApiService _apiService = ApiService();

  List<String> get _candidateUrls => [
    AppConstants.baseUrl,
    AppConstants.localAndroidUrl,
    AppConstants.localDesktopUrl,
    AppConstants.cloudBackendUrl,
  ];

  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  Future<User?> getCurrentUser() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString(_tokenKey);

    if (token != null && token.isNotEmpty) {
      _apiService.setAuthToken(token);
      // Try to fetch latest user profile from backend
      for (final base in _candidateUrls) {
        try {
          final res = await http.get(
            Uri.parse('$base/auth/me'),
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
              'Authorization': 'Bearer $token',
            },
          ).timeout(const Duration(milliseconds: AppConstants.connectTimeout));

          if (res.statusCode == 200) {
            final data = jsonDecode(res.body);
            final user = User.fromJson(data);
            await prefs.setString(_userKey, jsonEncode(user.toJson()));
            return user;
          }
        } catch (_) {}
      }
    }

    final userData = prefs.getString(_userKey);
    if (userData != null) {
      try {
        return User.fromJson(jsonDecode(userData));
      } catch (_) {}
    }
    return null;
  }

  Future<User> login({required String emailOrPhone, required String password}) async {
    final body = jsonEncode({
      'email': emailOrPhone.trim(),
      'password': password,
    });

    String? lastError;
    for (final base in _candidateUrls) {
      try {
        final res = await http.post(
          Uri.parse('$base/auth/login'),
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
          body: body,
        ).timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (res.statusCode == 200) {
          final data = jsonDecode(res.body);
          final token = data['token'] ?? data['accessToken'];
          User user;
          if (data['user'] != null) {
            user = User.fromJson(data['user']);
          } else {
            user = User(
              id: data['id']?.toString() ?? 'usr_${DateTime.now().millisecondsSinceEpoch}',
              name: data['name'] ?? emailOrPhone.split('@').first,
              email: data['email'] ?? emailOrPhone,
              phone: data['phone'] ?? '+91 98765 43210',
              walletBalance: (data['walletBalance'] ?? 250.0).toDouble(),
            );
          }
          await _persistSession(token, user);
          return user;
        } else {
          final err = jsonDecode(res.body);
          lastError = err['message'] ?? err['error'] ?? 'Login failed (${res.statusCode})';
        }
      } catch (e) {
        lastError = 'Network error connecting to backend: $e';
      }
    }

    throw Exception(lastError ?? 'Invalid credentials or server unreachable');
  }

  Future<User> register({
    required String name,
    required String email,
    required String phone,
    required String password,
  }) async {
    final body = jsonEncode({
      'name': name.trim(),
      'email': email.trim(),
      'phone': phone.trim(),
      'password': password,
    });

    String? lastError;
    for (final base in _candidateUrls) {
      try {
        final res = await http.post(
          Uri.parse('$base/auth/register'),
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
          body: body,
        ).timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (res.statusCode == 200 || res.statusCode == 201) {
          final data = jsonDecode(res.body);
          final token = data['token'] ?? data['accessToken'] ?? 'jwt_${DateTime.now().millisecondsSinceEpoch}';
          User user;
          if (data['user'] != null) {
            user = User.fromJson(data['user']);
          } else {
            user = User(
              id: data['id']?.toString() ?? 'usr_${DateTime.now().millisecondsSinceEpoch}',
              name: name,
              email: email,
              phone: phone,
              walletBalance: 200.0,
            );
          }
          await _persistSession(token, user);
          return user;
        } else {
          final err = jsonDecode(res.body);
          lastError = err['message'] ?? err['error'] ?? 'Registration failed';
        }
      } catch (e) {
        lastError = 'Network error connecting to backend: $e';
      }
    }

    throw Exception(lastError ?? 'Could not complete registration');
  }

  Future<User> updateUser(User user, {String? password}) async {
    final token = await getToken();
    if (token != null) {
      final body = jsonEncode({
        'name': user.name,
        'phone': user.phone,
        if (password != null && password.isNotEmpty) 'password': password,
      });

      for (final base in _candidateUrls) {
        try {
          final res = await http.put(
            Uri.parse('$base/auth/profile'),
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
              'Authorization': 'Bearer $token',
            },
            body: body,
          ).timeout(const Duration(milliseconds: AppConstants.connectTimeout));

          if (res.statusCode == 200) {
            final data = jsonDecode(res.body);
            final updated = User.fromJson(data);
            final prefs = await SharedPreferences.getInstance();
            await prefs.setString(_userKey, jsonEncode(updated.toJson()));
            return updated;
          }
        } catch (_) {}
      }
    }

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_userKey, jsonEncode(user.toJson()));
    return user;
  }

  Future<List<SavedTraveller>> getSavedTravellers() async {
    final token = await getToken();
    if (token != null) {
      for (final base in _candidateUrls) {
        try {
          final res = await http.get(
            Uri.parse('$base/auth/saved-travellers'),
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
              'Authorization': 'Bearer $token',
            },
          ).timeout(const Duration(milliseconds: AppConstants.connectTimeout));

          if (res.statusCode == 200) {
            final List data = jsonDecode(res.body);
            return data.map((e) => SavedTraveller.fromJson(e)).toList();
          }
        } catch (_) {}
      }
    }
    return [];
  }

  Future<SavedTraveller?> addSavedTraveller(SavedTraveller traveller) async {
    final token = await getToken();
    if (token != null) {
      final body = jsonEncode({
        'name': traveller.name,
        'age': traveller.age,
        'gender': traveller.gender,
      });

      for (final base in _candidateUrls) {
        try {
          final res = await http.post(
            Uri.parse('$base/auth/saved-travellers'),
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
              'Authorization': 'Bearer $token',
            },
            body: body,
          ).timeout(const Duration(milliseconds: AppConstants.connectTimeout));

          if (res.statusCode == 200 || res.statusCode == 201) {
            final data = jsonDecode(res.body);
            return SavedTraveller.fromJson(data);
          }
        } catch (_) {}
      }
    }
    return traveller;
  }

  Future<void> deleteSavedTraveller(String id) async {
    final token = await getToken();
    if (token != null) {
      for (final base in _candidateUrls) {
        try {
          await http.delete(
            Uri.parse('$base/auth/saved-travellers/$id'),
            headers: {
              'Authorization': 'Bearer $token',
            },
          ).timeout(const Duration(milliseconds: AppConstants.connectTimeout));
          break;
        } catch (_) {}
      }
    }
  }

  Future<void> forgotPassword(String email) async {
    final body = jsonEncode({'email': email.trim()});
    for (final base in _candidateUrls) {
      try {
        await http.post(
          Uri.parse('$base/auth/forgot-password'),
          headers: {'Content-Type': 'application/json'},
          body: body,
        ).timeout(const Duration(milliseconds: AppConstants.connectTimeout));
        break;
      } catch (_) {}
    }
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
