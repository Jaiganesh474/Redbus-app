import 'package:flutter/material.dart';
import '../models/bus_model.dart';
import '../models/seat_model.dart';
import '../models/booking_model.dart';
import '../models/coupon_model.dart';
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

  List<Booking> _myBookings = [];
  Booking? _lastConfirmedBooking;
  bool _isLoading = false;
  bool _isSeatsLoading = false;

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
  bool get isSeatsLoading => _isSeatsLoading;

  BookingProvider() {
    fetchMyBookings();
  }

  Future<void> fetchMyBookings() async {
    try {
      final list = await _apiService.getMyBookings();
      if (list.isNotEmpty) {
        _myBookings = list;
        notifyListeners();
      }
    } catch (_) {}
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

  void selectBus(Bus bus) async {
    _selectedBus = bus;
    _selectedSeats.clear();
    _passengers.clear();
    _selectedBoardingPoint = bus.boardingPoints.isNotEmpty ? bus.boardingPoints.first : null;
    _selectedDroppingPoint = bus.droppingPoints.isNotEmpty ? bus.droppingPoints.first : null;
    _appliedCoupon = null;
    _isSeatsLoading = true;
    notifyListeners();

    try {
      _busSeats = await _apiService.getSeatLayout(bus.id, bus.basePrice);
    } catch (_) {
      _busSeats = [];
    } finally {
      _isSeatsLoading = false;
      notifyListeners();
    }
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

    final routeIdNum = int.tryParse(_selectedBus!.id) ?? 1;
    final bookingPayload = {
      'routeId': routeIdNum,
      'boardingPoint': _selectedBoardingPoint?.name ?? 'Main Terminal',
      'droppingPoint': _selectedDroppingPoint?.name ?? 'City Drop',
      'contactEmail': _contactEmail,
      'contactPhone': _contactPhone,
      'passengers': _passengers.map((p) => {
        'name': p.name,
        'age': p.age,
        'gender': p.gender.toUpperCase(),
        'seatNumber': p.seatNumber,
        'price': p.seatPrice,
      }).toList(),
      'couponCode': _appliedCoupon?.code,
      'hasTripGuarantee': _optedInsurance,
      'serviceFee': gstAndTaxes,
      'totalAmount': totalPayableAmount,
    };

    final result = await _apiService.createBooking(bookingPayload);
    final pnr = result['pnr'] ?? 'RB${DateTime.now().millisecondsSinceEpoch.toString().substring(4)}';

    // Trigger confirmation email
    _apiService.sendTicketEmail(pnr, email: _contactEmail);

    final confirmedBooking = Booking(
      id: result['id']?.toString() ?? 'bk_${DateTime.now().millisecondsSinceEpoch}',
      pnr: pnr,
      tripInstanceId: _selectedBus!.tripInstanceId,
      busId: _selectedBus!.id,
      operatorName: _selectedBus!.operatorName,
      busType: _selectedBus!.busType,
      busNumber: _selectedBus!.busNumber,
      sourceCity: _selectedBus!.sourceCity,
      destinationCity: _selectedBus!.destinationCity,
      travelDate: result['travelDate']?.toString() ?? DateTime.now().toString().split(' ').first,
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
      paymentId: result['paymentId'] ?? 'pay_${DateTime.now().millisecondsSinceEpoch}',
      paymentMethod: _selectedPaymentMethod,
      status: BookingStatus.confirmed,
      createdAt: DateTime.now(),
      qrData: result['qrData'] ?? 'REDBUS:PNR=$pnr&PASSENGERS=${_selectedSeats.length}',
    );

    _myBookings.insert(0, confirmedBooking);
    _lastConfirmedBooking = confirmedBooking;
    _isLoading = false;
    notifyListeners();
    return confirmedBooking;
  }

  Future<void> cancelBookingByPnr(String pnr, {String reason = 'Customer cancellation'}) async {
    _isLoading = true;
    notifyListeners();

    try {
      await _apiService.cancelBooking(pnr, reason: reason);
      final index = _myBookings.indexWhere((b) => b.pnr == pnr);
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
      }
    } catch (_) {}
    _isLoading = false;
    notifyListeners();
  }
}
