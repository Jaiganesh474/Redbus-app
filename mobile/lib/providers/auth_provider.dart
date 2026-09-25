import 'package:flutter/material.dart';
import '../models/user_model.dart';
import '../services/auth_service.dart';

class AuthProvider with ChangeNotifier {
  final AuthService _authService = AuthService();

  User? _user;
  bool _isLoading = false;
  String? _errorMessage;

  User? get user => _user;
  bool get isAuthenticated => _user != null;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  AuthProvider() {
    loadUserSession();
  }

  Future<void> loadUserSession() async {
    _isLoading = true;
    notifyListeners();
    try {
      _user = await _authService.getCurrentUser();
    } catch (e) {
      debugPrint('Failed to load user: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> login(String emailOrPhone, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    try {
      _user = await _authService.login(emailOrPhone: emailOrPhone, password: password);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> register({
    required String name,
    required String email,
    required String phone,
    required String password,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    try {
      _user = await _authService.register(
        name: name,
        email: email,
        phone: phone,
        password: password,
      );
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<void> updateProfile({String? name, String? phone, String? avatarUrl, String? gender}) async {
    if (_user == null) return;
    _user = _user!.copyWith(
      name: name ?? _user!.name,
      phone: phone ?? _user!.phone,
      avatarUrl: avatarUrl ?? _user!.avatarUrl,
      gender: gender ?? _user!.gender,
    );
    await _authService.updateUser(_user!);
    notifyListeners();
  }

  Future<void> addSavedTraveller(SavedTraveller traveller) async {
    if (_user == null) return;
    final updatedList = List<SavedTraveller>.from(_user!.savedTravellers)..add(traveller);
    _user = _user!.copyWith(savedTravellers: updatedList);
    await _authService.updateUser(_user!);
    notifyListeners();
  }

  Future<void> removeSavedTraveller(String id) async {
    if (_user == null) return;
    final updatedList = _user!.savedTravellers.where((t) => t.id != id).toList();
    _user = _user!.copyWith(savedTravellers: updatedList);
    await _authService.updateUser(_user!);
    notifyListeners();
  }

  Future<void> logout() async {
    await _authService.logout();
    _user = null;
    notifyListeners();
  }
}
