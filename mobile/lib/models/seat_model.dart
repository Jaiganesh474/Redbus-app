enum SeatType {
  seater,
  sleeper,
}

enum DeckType {
  lower,
  upper,
}

enum SeatStatus {
  available,
  booked,
  locked,
  selected,
  femaleOnly,
  maleOnly,
}

class Seat {
  final String id;
  final String seatNumber;
  final int row;
  final int column;
  final DeckType deck;
  final SeatType type;
  final double price;
  SeatStatus status;
  final bool isWindow;
  final bool isAisle;

  Seat({
    required this.id,
    required this.seatNumber,
    required this.row,
    required this.column,
    required this.deck,
    required this.type,
    required this.price,
    this.status = SeatStatus.available,
    this.isWindow = false,
    this.isAisle = false,
  });

  bool get isAvailable => status == SeatStatus.available || status == SeatStatus.femaleOnly;
  bool get isSelected => status == SeatStatus.selected;
  bool get isBooked => status == SeatStatus.booked || status == SeatStatus.locked;
  bool get isFemale => status == SeatStatus.femaleOnly;

  factory Seat.fromJson(Map<String, dynamic> json) {
    SeatStatus parseStatus(String? s) {
      switch (s?.toUpperCase()) {
        case 'AVAILABLE':
          return SeatStatus.available;
        case 'BOOKED':
          return SeatStatus.booked;
        case 'LOCKED':
          return SeatStatus.locked;
        case 'FEMALE_ONLY':
          return SeatStatus.femaleOnly;
        default:
          return SeatStatus.available;
      }
    }

    return Seat(
      id: json['id']?.toString() ?? '',
      seatNumber: json['seatNumber'] ?? json['seat_number'] ?? '',
      row: json['row'] ?? 0,
      column: json['column'] ?? 0,
      deck: (json['deck']?.toString().toLowerCase() == 'upper') ? DeckType.upper : DeckType.lower,
      type: (json['type']?.toString().toLowerCase() == 'sleeper') ? SeatType.sleeper : SeatType.seater,
      price: (json['price'] ?? 0).toDouble(),
      status: parseStatus(json['status']),
      isWindow: json['isWindow'] ?? false,
      isAisle: json['isAisle'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'seatNumber': seatNumber,
      'row': row,
      'column': column,
      'deck': deck == DeckType.upper ? 'UPPER' : 'LOWER',
      'type': type == SeatType.sleeper ? 'SLEEPER' : 'SEATER',
      'price': price,
      'status': status.name.toUpperCase(),
      'isWindow': isWindow,
      'isAisle': isAisle,
    };
  }

  Seat copyWith({
    String? id,
    String? seatNumber,
    int? row,
    int? column,
    DeckType? deck,
    SeatType? type,
    double? price,
    SeatStatus? status,
    bool? isWindow,
    bool? isAisle,
  }) {
    return Seat(
      id: id ?? this.id,
      seatNumber: seatNumber ?? this.seatNumber,
      row: row ?? this.row,
      column: column ?? this.column,
      deck: deck ?? this.deck,
      type: type ?? this.type,
      price: price ?? this.price,
      status: status ?? this.status,
      isWindow: isWindow ?? this.isWindow,
      isAisle: isAisle ?? this.isAisle,
    );
  }
}
