import 'package:flutter/material.dart';
import '../models/bus_model.dart';
import '../models/seat_model.dart';
import '../models/booking_model.dart';
import '../models/coupon_model.dart';
import '../services/mock_data_service.dart';
import '../services/api_service.dart';

class BookingProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  Bus? _selectedBus;
  List<Seat> _busSeats = [];
  final List<Seat> _selectedSeats = [];
  DeckType _activeDeck = DeckType.lower;

  BoardingDroppingPoint? _selectedBoardingPoint;
  BoardingDroppingPoint? _selectedDroppingPoint;

  final List<PassengerInfo> _passengers = [];
  String _contactPhone = '9876543210';
  String _contactEmail = 'traveler@redbus.in';
  bool _optedInsurance = true;

  Coupon? _appliedCoupon;
  String _selectedPaymentMethod = 'UPI';

  // Booking history
  List<Booking> _myBookings = [];
  Booking? _lastConfirmedBooking;
  bool _isLoading = false;

  // Getters
  Bus? get selectedBus => _selectedBus;
  List<Seat> get busSeats => _busSeats;
  List<Seat> get selectedSeats => _selectedSeats;
  DeckType get activeDeck => _activeDeck;
  BoardingDroppingPoint? get selectedBoardingPoint => _selectedBoardingPoint;
  BoardingDroppingPoint? get selectedDroppingPoint => _selectedDroppingPoint;
  List<PassengerInfo> get passengers => _passengers;
  String get contactPhone => _contactPhone;
  String get contactEmail => _contactEmail;
  bool get optedInsurance => _optedInsurance;
  Coupon? get appliedCoupon => _appliedCoupon;
  String get selectedPaymentMethod => _selectedPaymentMethod;
  List<Booking> get myBookings => _myBookings;
  Booking? get lastConfirmedBooking => _lastConfirmedBooking;
  bool get isLoading => _isLoading;

  BookingProvider() {
    _myBookings = MockDataService.getMockBookings();
  }

  // Fare calculations
  double get baseFare {
    return _selectedSeats.fold(0.0, (sum, seat) => sum + seat.price);
  }

  double get gstAndTaxes {
    return baseFare * 0.05; // 5% GST on bus travel
  }

  double get insuranceFee {
    return _optedInsurance ? (_selectedSeats.length * 15.0) : 0.0;
  }

  double get discountAmount {
    if (_appliedCoupon == null) return 0.0;
    return _appliedCoupon!.calculateDiscount(baseFare);
  }

  double get totalPayableAmount {
    final total = baseFare + gstAndTaxes + insuranceFee - discountAmount;
    return total > 0 ? total : 0;
  }

  void selectBus(Bus bus) {
    _selectedBus = bus;
    _busSeats = bus.seats.isNotEmpty ? bus.seats : MockDataService.generateSeatLayout(bus.basePrice);
    _selectedSeats.clear();
    _passengers.clear();
    _selectedBoardingPoint = bus.boardingPoints.isNotEmpty ? bus.boardingPoints.first : null;
    _selectedDroppingPoint = bus.droppingPoints.isNotEmpty ? bus.droppingPoints.first : null;
    _appliedCoupon = null;
    notifyListeners();
  }

  void setActiveDeck(DeckType deck) {
    _activeDeck = deck;
    notifyListeners();
  }

  void toggleSeatSelection(Seat seat) {
    if (seat.isBooked) return;

    final existingIndex = _selectedSeats.indexWhere((s) => s.id == seat.id);
    if (existingIndex >= 0) {
      _selectedSeats.removeAt(existingIndex);
      seat.status = SeatStatus.available;
    } else {
      if (_selectedSeats.length >= 6) return; // max 6 seats per booking
      _selectedSeats.add(seat);
      seat.status = SeatStatus.selected;
    }
    notifyListeners();
  }

  void setBoardingPoint(BoardingDroppingPoint point) {
    _selectedBoardingPoint = point;
    notifyListeners();
  }

  void setDroppingPoint(BoardingDroppingPoint point) {
    _selectedDroppingPoint = point;
    notifyListeners();
  }

  void setContactInfo(String phone, String email) {
    _contactPhone = phone;
    _contactEmail = email;
    notifyListeners();
  }

  void toggleInsurance(bool value) {
    _optedInsurance = value;
    notifyListeners();
  }

  void applyCoupon(Coupon coupon) {
    _appliedCoupon = coupon;
    notifyListeners();
  }

  void removeCoupon() {
    _appliedCoupon = null;
    notifyListeners();
  }

  void setPaymentMethod(String method) {
    _selectedPaymentMethod = method;
    notifyListeners();
  }

  void updatePassenger(int index, PassengerInfo passenger) {
    if (index < _passengers.length) {
      _passengers[index] = passenger;
    } else {
      _passengers.add(passenger);
    }
    notifyListeners();
  }

  Future<Booking?> confirmAndPay() async {
    if (_selectedBus == null || _selectedSeats.isEmpty) return null;

    _isLoading = true;
    notifyListeners();

    // Prepare passenger info if not fully mapped
    if (_passengers.length < _selectedSeats.length) {
      for (int i = _passengers.length; i < _selectedSeats.length; i++) {
        _passengers.add(PassengerInfo(
          name: 'Passenger ${i + 1}',
          age: 26,
          gender: 'Male',
          seatNumber: _selectedSeats[i].seatNumber,
          seatPrice: _selectedSeats[i].price,
        ));
      }
    }

    final bookingPayload = {
      'busId': _selectedBus!.id,
      'tripInstanceId': _selectedBus!.tripInstanceId,
      'sourceCity': _selectedBus!.sourceCity,
      'destinationCity': _selectedBus!.destinationCity,
      'travelDate': DateTime.now().add(const Duration(days: 1)).toString().split(' ')[0],
      'boardingPoint': _selectedBoardingPoint?.toJson(),
      'droppingPoint': _selectedDroppingPoint?.toJson(),
      'seats': _selectedSeats.map((s) => s.toJson()).toList(),
      'passengers': _passengers.map((p) => p.toJson()).toList(),
      'baseFare': baseFare,
      'taxAndGst': gstAndTaxes,
      'discountAmount': discountAmount,
      'insuranceFee': insuranceFee,
      'totalAmount': totalPayableAmount,
      'paymentMethod': _selectedPaymentMethod,
    };

    final result = await _apiService.createBooking(bookingPayload);
    final pnr = result['pnr'] ?? 'RB${DateTime.now().millisecondsSinceEpoch.toString().substring(4)}';

    final confirmedBooking = Booking(
      id: result['id'] ?? 'bk_${DateTime.now().millisecondsSinceEpoch}',
      pnr: pnr,
      tripInstanceId: _selectedBus!.tripInstanceId,
      busId: _selectedBus!.id,
      operatorName: _selectedBus!.operatorName,
      busType: _selectedBus!.busType,
      busNumber: _selectedBus!.busNumber,
      sourceCity: _selectedBus!.sourceCity,
      destinationCity: _selectedBus!.destinationCity,
      travelDate: 'Tomorrow, ${DateTime.now().add(const Duration(days: 1)).day}',
      departureTime: _selectedBus!.departureTime,
      arrivalTime: _selectedBus!.arrivalTime,
      boardingPoint: _selectedBoardingPoint ?? _selectedBus!.boardingPoints.first,
      droppingPoint: _selectedDroppingPoint ?? _selectedBus!.droppingPoints.first,
      passengers: List.from(_passengers),
      selectedSeats: List.from(_selectedSeats),
      baseFare: baseFare,
      taxAndGst: gstAndTaxes,
      discountAmount: discountAmount,
      insuranceFee: insuranceFee,
      totalAmount: totalPayableAmount,
      paymentId: result['paymentId'] ?? 'pay_mock_${DateTime.now().millisecondsSinceEpoch}',
      paymentMethod: _selectedPaymentMethod,
      status: BookingStatus.confirmed,
      createdAt: DateTime.now(),
      qrData: 'REDBUS:PNR=$pnr&PASSENGERS=${_selectedSeats.length}',
    );

    _myBookings.insert(0, confirmedBooking);
    _lastConfirmedBooking = confirmedBooking;
    _isLoading = false;
    notifyListeners();
    return confirmedBooking;
  }

  void cancelBooking(String bookingId) {
    final index = _myBookings.indexWhere((b) => b.id == bookingId);
    if (index >= 0) {
      final old = _myBookings[index];
      _myBookings[index] = Booking(
        id: old.id,
        pnr: old.pnr,
        tripInstanceId: old.tripInstanceId,
        busId: old.busId,
        operatorName: old.operatorName,
        busType: old.busType,
        busNumber: old.busNumber,
        sourceCity: old.sourceCity,
        destinationCity: old.destinationCity,
        travelDate: old.travelDate,
        departureTime: old.departureTime,
        arrivalTime: old.arrivalTime,
        boardingPoint: old.boardingPoint,
        droppingPoint: old.droppingPoint,
        passengers: old.passengers,
        selectedSeats: old.selectedSeats,
        baseFare: old.baseFare,
        taxAndGst: old.taxAndGst,
        discountAmount: old.discountAmount,
        insuranceFee: old.insuranceFee,
        totalAmount: old.totalAmount,
        paymentId: old.paymentId,
        paymentMethod: old.paymentMethod,
        status: BookingStatus.cancelled,
        createdAt: old.createdAt,
        qrData: old.qrData,
      );
      notifyListeners();
    }
  }
}
