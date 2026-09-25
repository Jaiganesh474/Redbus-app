import '../models/bus_model.dart';
import '../models/seat_model.dart';
import '../models/booking_model.dart';
import '../models/coupon_model.dart';
import '../models/user_model.dart';

class MockDataService {
  static List<Seat> generateSeatLayout(double basePrice) {
    List<Seat> seats = [];
    int seatCounter = 1;

    // Lower Deck (Seater & Lower Sleeper) - 4 rows x 3 cols (2 + 1)
    for (int row = 1; row <= 5; row++) {
      // Left side: 2 seats
      for (int col = 1; col <= 2; col++) {
        final seatNo = 'L$seatCounter';
        final isFemale = seatCounter == 3 || seatCounter == 4;
        final isBooked = seatCounter == 1 || seatCounter == 7 || seatCounter == 9;

        seats.add(Seat(
          id: 'seat_l_$seatCounter',
          seatNumber: seatNo,
          row: row,
          column: col,
          deck: DeckType.lower,
          type: SeatType.seater,
          price: basePrice,
          status: isBooked
              ? SeatStatus.booked
              : (isFemale ? SeatStatus.femaleOnly : SeatStatus.available),
          isWindow: col == 1,
          isAisle: col == 2,
        ));
        seatCounter++;
      }

      // Right side: 1 single sleeper/seater
      final singleSeatNo = 'L$seatCounter';
      final isSingleBooked = seatCounter == 6 || seatCounter == 12;
      seats.add(Seat(
        id: 'seat_l_$seatCounter',
        seatNumber: singleSeatNo,
        row: row,
        column: 4,
        deck: DeckType.lower,
        type: SeatType.sleeper,
        price: basePrice + 150,
        status: isSingleBooked ? SeatStatus.booked : SeatStatus.available,
        isWindow: true,
        isAisle: false,
      ));
      seatCounter++;
    }

    // Upper Deck (Sleeper berths) - 5 rows x 3 cols (2 + 1)
    int upperCounter = 1;
    for (int row = 1; row <= 5; row++) {
      // Left side berths (Double Berth)
      for (int col = 1; col <= 2; col++) {
        final seatNo = 'U$upperCounter';
        final isBooked = upperCounter == 2 || upperCounter == 5 || upperCounter == 11;
        final isFemale = upperCounter == 4;

        seats.add(Seat(
          id: 'seat_u_$upperCounter',
          seatNumber: seatNo,
          row: row,
          column: col,
          deck: DeckType.upper,
          type: SeatType.sleeper,
          price: basePrice + 250,
          status: isBooked
              ? SeatStatus.booked
              : (isFemale ? SeatStatus.femaleOnly : SeatStatus.available),
          isWindow: col == 1,
          isAisle: col == 2,
        ));
        upperCounter++;
      }

      // Right side single sleeper berth
      final singleSeatNo = 'U$upperCounter';
      final isSingleBooked = upperCounter == 3 || upperCounter == 8;
      seats.add(Seat(
        id: 'seat_u_$upperCounter',
        seatNumber: singleSeatNo,
        row: row,
        column: 4,
        deck: DeckType.upper,
        type: SeatType.sleeper,
        price: basePrice + 350,
        status: isSingleBooked ? SeatStatus.booked : SeatStatus.available,
        isWindow: true,
        isAisle: false,
      ));
      upperCounter++;
    }

    return seats;
  }

  static List<Bus> getBusesForRoute(String source, String destination, String date) {
    return [
      Bus(
        id: 'bus_1',
        tripInstanceId: 'trip_101',
        operatorName: 'IntrCity SmartBus',
        busType: 'AC Sleeper (2+1) Multi-Axle',
        busNumber: 'KA-01-AK-2024',
        departureTime: '21:15',
        arrivalTime: '06:00',
        duration: '8h 45m',
        sourceCity: source,
        destinationCity: destination,
        basePrice: 850.0,
        rating: 4.8,
        totalRatings: 1420,
        availableSeats: 16,
        totalSeats: 30,
        isPrimo: true,
        isLiveTracking: true,
        amenities: [
          'Free Wi-Fi',
          'Charging Point',
          'Water Bottle',
          'Blanket & Pillow',
          'Live Tracking',
          'CCTV Surveillance',
          'Reading Light'
        ],
        photos: [
          'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800',
          'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=800',
        ],
        boardingPoints: [
          BoardingDroppingPoint(id: 'b1', name: 'Madiwala', time: '21:15', landmark: 'Near Police Station', address: 'Hosur Main Road, Madiwala'),
          BoardingDroppingPoint(id: 'b2', name: 'Silk Board', time: '21:30', landmark: 'Under Flyover', address: 'Central Silk Board Junction'),
          BoardingDroppingPoint(id: 'b3', name: 'Electronic City', time: '21:55', landmark: 'Toll Plaza Gate 2', address: 'Hosur Road, Phase 1'),
          BoardingDroppingPoint(id: 'b4', name: 'Majestic', time: '20:30', landmark: 'Near Platform 18', address: 'KSRTC Bus Stand Majestic'),
        ],
        droppingPoints: [
          BoardingDroppingPoint(id: 'd1', name: 'Gachibowli', time: '05:30', landmark: 'ORR Junction', address: 'Outer Ring Road, Gachibowli'),
          BoardingDroppingPoint(id: 'd2', name: 'Ameerpet', time: '06:00', landmark: 'Metro Pillar 1042', address: 'Ameerpet Main Road'),
          BoardingDroppingPoint(id: 'd3', name: 'MGBS Terminal', time: '06:30', landmark: 'Platform 3', address: 'Mahatma Gandhi Bus Station'),
        ],
        reviews: [
          BusReview(
            id: 'r1',
            userName: 'Rahul Verma',
            rating: 5.0,
            comment: 'Very clean bus and departed on the exact minute. Blanket was freshly packed.',
            date: '2 days ago',
            tags: ['Punctual', 'Clean', 'Comfortable'],
          ),
          BusReview(
            id: 'r2',
            userName: 'Priya Sharma',
            rating: 4.5,
            comment: 'Smooth driving and safe for solo female travellers. Staff was courteous.',
            date: '1 week ago',
            tags: ['Safe for Women', 'Courteous Staff'],
          ),
        ],
        cancellationPolicies: [
          CancellationPolicy(timeFrame: 'Before 24 hrs of departure', refundPercent: '90% refund'),
          CancellationPolicy(timeFrame: 'Between 12 to 24 hrs', refundPercent: '70% refund'),
          CancellationPolicy(timeFrame: 'Between 4 to 12 hrs', refundPercent: '50% refund'),
          CancellationPolicy(timeFrame: 'Under 4 hrs', refundPercent: '0% refund'),
        ],
        seats: generateSeatLayout(850.0),
      ),
      Bus(
        id: 'bus_2',
        tripInstanceId: 'trip_102',
        operatorName: 'Zingbus Electric Lounge',
        busType: 'Premium Electric AC Sleeper',
        busNumber: 'DL-01-EL-8899',
        departureTime: '22:00',
        arrivalTime: '06:30',
        duration: '8h 30m',
        sourceCity: source,
        destinationCity: destination,
        basePrice: 920.0,
        rating: 4.7,
        totalRatings: 980,
        availableSeats: 12,
        totalSeats: 30,
        isPrimo: true,
        isLiveTracking: true,
        amenities: [
          'Ultra Silent Cabin',
          'Free Wi-Fi',
          'Charging Point',
          'Mineral Water',
          'Air Purifier',
          'Reading Light'
        ],
        boardingPoints: [
          BoardingDroppingPoint(id: 'b21', name: 'Majestic Bus Stand', time: '22:00', landmark: 'Opposite Railway Station', address: 'Platform 12'),
          BoardingDroppingPoint(id: 'b22', name: 'Hebbal Flyover', time: '22:30', landmark: 'Near Esteem Mall', address: 'Bellary Road'),
        ],
        droppingPoints: [
          BoardingDroppingPoint(id: 'd21', name: 'Shamshabad Airport', time: '05:45', landmark: 'Decathlon Store', address: 'NH 44 Airport Road'),
          BoardingDroppingPoint(id: 'd22', name: 'Kukatpally', time: '06:30', landmark: 'Y Junction', address: 'KPHB Colony'),
        ],
        reviews: [
          BusReview(
            id: 'r21',
            userName: 'Anand Kumar',
            rating: 5.0,
            comment: 'Electric bus was noiseless and vibration free. Loved the lounge seats!',
            date: '3 days ago',
            tags: ['Super Silent', 'Lounge Berth'],
          ),
        ],
        cancellationPolicies: [
          CancellationPolicy(timeFrame: 'Before 24 hrs', refundPercent: '90% refund'),
          CancellationPolicy(timeFrame: 'Under 12 hrs', refundPercent: '50% refund'),
        ],
        seats: generateSeatLayout(920.0),
      ),
      Bus(
        id: 'bus_3',
        tripInstanceId: 'trip_103',
        operatorName: 'Orange Travels Express',
        busType: 'Scania Multi-Axle AC Sleeper',
        busNumber: 'AP-09-OT-3344',
        departureTime: '20:45',
        arrivalTime: '05:15',
        duration: '8h 30m',
        sourceCity: source,
        destinationCity: destination,
        basePrice: 780.0,
        rating: 4.4,
        totalRatings: 2150,
        availableSeats: 21,
        totalSeats: 30,
        isPrimo: false,
        isLiveTracking: true,
        amenities: [
          'Charging Point',
          'Water Bottle',
          'Blanket',
          'Live Tracking',
          'Emergency Exit'
        ],
        boardingPoints: [
          BoardingDroppingPoint(id: 'b31', name: 'Indiranagar', time: '20:45', landmark: 'Near CMH Road Metro', address: '100 Feet Road'),
          BoardingDroppingPoint(id: 'b32', name: 'Tin Factory', time: '21:10', landmark: 'Pedestrian Bridge', address: 'Old Madras Road'),
        ],
        droppingPoints: [
          BoardingDroppingPoint(id: 'd31', name: 'Hitech City', time: '05:00', landmark: 'Cyber Towers', address: 'Madhapur'),
          BoardingDroppingPoint(id: 'd32', name: 'Secunderabad', time: '05:30', landmark: 'Railway Station', address: 'Station Road'),
        ],
        reviews: [
          BusReview(
            id: 'r31',
            userName: 'Deepak N',
            rating: 4.0,
            comment: 'Decent journey, on-time drop at Hitech City.',
            date: 'Yesterday',
            tags: ['On Time'],
          ),
        ],
        cancellationPolicies: [
          CancellationPolicy(timeFrame: 'Before 12 hrs', refundPercent: '80% refund'),
        ],
        seats: generateSeatLayout(780.0),
      ),
      Bus(
        id: 'bus_4',
        tripInstanceId: 'trip_104',
        operatorName: 'SRS Travels',
        busType: 'Non-AC Sleeper / Seater (2+1)',
        busNumber: 'KA-05-SR-1122',
        departureTime: '22:30',
        arrivalTime: '07:30',
        duration: '9h 00m',
        sourceCity: source,
        destinationCity: destination,
        basePrice: 550.0,
        rating: 4.1,
        totalRatings: 840,
        availableSeats: 19,
        totalSeats: 30,
        isPrimo: false,
        isLiveTracking: true,
        amenities: [
          'Charging Point',
          'Emergency Exit',
          'Reading Light'
        ],
        boardingPoints: [
          BoardingDroppingPoint(id: 'b41', name: 'Majestic', time: '22:30', landmark: 'SRS Head Office', address: 'Majestic Circle'),
        ],
        droppingPoints: [
          BoardingDroppingPoint(id: 'd41', name: 'Lakdikapul', time: '07:30', landmark: 'Near Pillar 18', address: 'Main Road'),
        ],
        reviews: [],
        cancellationPolicies: [
          CancellationPolicy(timeFrame: 'Standard Policy', refundPercent: '75% refund'),
        ],
        seats: generateSeatLayout(550.0),
      ),
      Bus(
        id: 'bus_5',
        tripInstanceId: 'trip_105',
        operatorName: 'NueGo Intercity',
        busType: 'Luxury AC Seater (2+2)',
        busNumber: 'KA-51-NG-9001',
        departureTime: '18:30',
        arrivalTime: '02:45',
        duration: '8h 15m',
        sourceCity: source,
        destinationCity: destination,
        basePrice: 650.0,
        rating: 4.9,
        totalRatings: 3100,
        availableSeats: 8,
        totalSeats: 36,
        isPrimo: true,
        isLiveTracking: true,
        amenities: [
          'Free High-Speed Wi-Fi',
          'Individual USB Ports',
          'Snack Box',
          'Mineral Water',
          'CCTV',
          'GPS Tracking'
        ],
        boardingPoints: [
          BoardingDroppingPoint(id: 'b51', name: 'Shanthi Nagar Bus Station', time: '18:30', landmark: 'Double Road', address: 'KH Road'),
        ],
        droppingPoints: [
          BoardingDroppingPoint(id: 'd51', name: 'Mehdipatnam', time: '02:45', landmark: 'Near Pillar 52', address: 'PVNR Expressway'),
        ],
        reviews: [
          BusReview(
            id: 'r51',
            userName: 'Sneha Roy',
            rating: 5.0,
            comment: 'Best electric bus experience! Super comfy seats and polite hostess.',
            date: '4 days ago',
            tags: ['Primo', 'Top Service'],
          ),
        ],
        cancellationPolicies: [
          CancellationPolicy(timeFrame: 'Before 24 hrs', refundPercent: '100% refund'),
        ],
        seats: generateSeatLayout(650.0),
      ),
    ];
  }

  static List<Coupon> getCoupons() {
    return [
      Coupon(
        code: 'FIRST50',
        title: 'Flat 15% OFF for New Users',
        description: 'Get 15% instant discount up to ₹250 on your first booking',
        discountPercent: 15.0,
        maxDiscount: 250.0,
        minFare: 400.0,
        validTill: '31 Dec 2026',
        badge: 'NEW USER',
      ),
      Coupon(
        code: 'SUPERBUS',
        title: 'Save ₹150 on Primo Buses',
        description: 'Exclusive instant discount on all Primo verified fleet',
        discountPercent: 20.0,
        maxDiscount: 150.0,
        minFare: 500.0,
        validTill: '30 Nov 2026',
        badge: 'PRIMO',
      ),
      Coupon(
        code: 'REDBUS100',
        title: 'Flat ₹100 Instant Discount',
        description: 'Applicable on bookings above ₹500 across all routes',
        discountPercent: 10.0,
        maxDiscount: 100.0,
        minFare: 500.0,
        validTill: '15 Oct 2026',
        badge: 'POPULAR',
      ),
    ];
  }

  static User getMockUser() {
    return User(
      id: 'usr_1001',
      name: 'Antigravity Explorer',
      email: 'traveler@redbus.in',
      phone: '+91 98765 43210',
      avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200',
      gender: 'Male',
      role: 'ROLE_USER',
      walletBalance: 250.0,
      savedTravellers: [
        SavedTraveller(id: 'st_1', name: 'Antigravity Explorer', age: 26, gender: 'Male'),
        SavedTraveller(id: 'st_2', name: 'Aadhya Sharma', age: 24, gender: 'Female'),
        SavedTraveller(id: 'st_3', name: 'Ramesh Kumar', age: 52, gender: 'Male'),
      ],
    );
  }

  static List<Booking> getMockBookings() {
    return [
      Booking(
        id: 'bk_2001',
        pnr: 'RB892174301',
        tripInstanceId: 'trip_101',
        busId: 'bus_1',
        operatorName: 'IntrCity SmartBus',
        busType: 'AC Sleeper (2+1) Multi-Axle',
        busNumber: 'KA-01-AK-2024',
        sourceCity: 'Bangalore',
        destinationCity: 'Hyderabad',
        travelDate: '2026-09-25',
        departureTime: '21:15',
        arrivalTime: '06:00',
        boardingPoint: BoardingDroppingPoint(
          id: 'b1',
          name: 'Madiwala',
          time: '21:15',
          landmark: 'Near Police Station',
        ),
        droppingPoint: BoardingDroppingPoint(
          id: 'd1',
          name: 'Gachibowli',
          time: '05:30',
          landmark: 'ORR Junction',
        ),
        passengers: [
          PassengerInfo(name: 'Antigravity Explorer', age: 26, gender: 'Male', seatNumber: 'L4', seatPrice: 850.0),
          PassengerInfo(name: 'Aadhya Sharma', age: 24, gender: 'Female', seatNumber: 'L5', seatPrice: 850.0),
        ],
        baseFare: 1700.0,
        taxAndGst: 85.0,
        discountAmount: 250.0,
        insuranceFee: 30.0,
        totalAmount: 1565.0,
        paymentId: 'pay_rzp_mock_9921',
        paymentMethod: 'Google Pay / UPI',
        status: BookingStatus.confirmed,
        createdAt: DateTime.now().subtract(const Duration(hours: 4)),
        qrData: 'REDBUS:PNR=RB892174301&PASSENGERS=2&ROUTE=BLR-HYD',
      ),
      Booking(
        id: 'bk_2002',
        pnr: 'RB664019283',
        tripInstanceId: 'trip_105',
        busId: 'bus_5',
        operatorName: 'NueGo Intercity',
        busType: 'Luxury AC Seater (2+2)',
        busNumber: 'KA-51-NG-9001',
        sourceCity: 'Bangalore',
        destinationCity: 'Chennai',
        travelDate: '2026-09-10',
        departureTime: '18:30',
        arrivalTime: '02:45',
        boardingPoint: BoardingDroppingPoint(
          id: 'b51',
          name: 'Shanthi Nagar',
          time: '18:30',
          landmark: 'Double Road',
        ),
        droppingPoint: BoardingDroppingPoint(
          id: 'd51',
          name: 'Koyambedu',
          time: '02:45',
          landmark: 'Omni Bus Stand',
        ),
        passengers: [
          PassengerInfo(name: 'Antigravity Explorer', age: 26, gender: 'Male', seatNumber: 'S12', seatPrice: 650.0),
        ],
        baseFare: 650.0,
        taxAndGst: 32.5,
        discountAmount: 100.0,
        insuranceFee: 15.0,
        totalAmount: 597.5,
        paymentId: 'pay_rzp_mock_1120',
        paymentMethod: 'HDFC Credit Card',
        status: BookingStatus.completed,
        createdAt: DateTime.now().subtract(const Duration(days: 12)),
        qrData: 'REDBUS:PNR=RB664019283&PASSENGERS=1&ROUTE=BLR-MAA',
      ),
    ];
  }
}
