import 'package:flutter/material.dart';
import '../models/user_model.dart';
import '../services/auth_service.dart';

class AuthProvider with ChangeNotifier {
  final AuthService _authService = AuthService();

  User? _user;
  List<SavedTraveller> _savedTravellers = [];
  bool _isLoading = false;
  String? _errorMessage;

  User? get user => _user;
  List<SavedTraveller> get savedTravellers => _savedTravellers;
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
      if (_user != null) {
        _savedTravellers = await _authService.getSavedTravellers();
      }
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
      _savedTravellers = await _authService.getSavedTravellers();
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception:', '').trim();
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
      _savedTravellers = await _authService.getSavedTravellers();
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception:', '').trim();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateProfile({required String name, required String phone, String? password}) async {
    if (_user == null) return false;
    _isLoading = true;
    notifyListeners();
    try {
      final updated = _user!.copyWith(name: name, phone: phone);
      _user = await _authService.updateUser(updated, password: password);
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

  Future<void> addSavedTraveller(SavedTraveller traveller) async {
    try {
      final result = await _authService.addSavedTraveller(traveller);
      if (result != null) {
        _savedTravellers.add(result);
        notifyListeners();
      }
    } catch (_) {}
  }

  Future<void> removeSavedTraveller(String id) async {
    try {
      await _authService.deleteSavedTraveller(id);
      _savedTravellers.removeWhere((t) => t.id == id);
      notifyListeners();
    } catch (_) {}
  }

  Future<void> forgotPassword(String email) async {
    await _authService.forgotPassword(email);
  }

  Future<void> logout() async {
    await _authService.logout();
    _user = null;
    _savedTravellers.clear();
    notifyListeners();
  }
}
