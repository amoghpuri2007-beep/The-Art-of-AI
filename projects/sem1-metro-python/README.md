# 🚇 Delhi Metro Route & Schedule Simulator

> A 1st Semester Python project that simulates the Delhi Metro — real stations,  
> real travel times, real interchange logic. No external libraries. Pure Python.

---

## 📱 What Does This Project Do?

A terminal-based simulator for the Delhi Metro system with two core features:

1. **Metro Timings** — Enter any station and current time → get the next metro arrivals
2. **Journey Planner** — Enter source and destination → get the fastest route with interchanges, timing, and total travel time

---

## 🗺️ Lines Covered

| Line | Terminus A | Terminus B | Stations |
|------|-----------|-----------|----------|
| 🔵 Blue | Dwarka Sector 21 | Noida Electronic City / Vaishali | 57 |
| 🟣 Magenta | Krishna Park | Botanical Garden | 25 |
| ⚫ Grey | Dwarka (Delhi) | Dhansa Bus Stand | 4 |

> All station data sourced directly from **official DMRC records** and stored in `metro_data.txt`

---

## 🔄 How It Works

```
Run metro_simulator.py
        │
        ▼
┌───────────────────┐
│     Main Menu     │
├───────────────────┤
│ 1. Metro Timings  │──▶ Enter line + station + time → Next 6 arrivals
│ 2. Journey Planner│──▶ Enter source + destination → Full route plan
│ 3. Exit           │
└───────────────────┘
```

---

## 🧩 Features Breakdown

### ⏱️ Metro Timings Module
- Enter any station name and current time
- App calculates when the **next metro will arrive** based on:
  - Peak hours (8–10 AM, 5–7 PM) → every **4 minutes**
  - Off-peak hours → every **8 minutes**
  - Service hours: **6:00 AM – 11:00 PM**
- Returns next 6 upcoming arrivals

### 🗺️ Journey Planner
Handles **three types of journeys automatically:**

| Journey Type | Example | How it works |
|---|---|---|
| Direct | Rajiv Chowk → Barakhamba Road | Single line, no transfer |
| Single interchange | Dwarka Sec 8 → Dhansa Bus Stand | Blue → Grey at Dwarka |
| Double interchange | Grey → Blue → Magenta | Two transfers, fastest path selected |

- Adds **3 minute interchange delay** at every transfer station
- Automatically compares all possible routes and picks the **fastest one**
- Displays the full route: `Source → Interchange → Destination`

### 🔍 Smart Station Name Matching
- **Case-insensitive** — `rajiv chowk` matches `Rajiv Chowk`
- Handles numbered input — `29. Rajiv Chowk` works fine
- Strips interchange markers — `Dwarka [I]` is understood
- **Partial matching** as fallback — reduces "station not found" errors

---

## 💻 Sample Output

### Metro Timings
```
Line: Blue | Station: Rajiv Chowk | Time: 09:18

Next metro at: 09:20
Upcoming: 09:24, 09:28, 09:32, 09:36, 09:40
```

### Journey Planner — Direct Route
```
Source: Rajiv Chowk → Destination: Barakhamba Road | Time: 08:22

Journey Plan:
Start at Rajiv Chowk (Blue Line)
Next metro at 08:24
Arrive at Barakhamba Road at 08:25
Total travel time: 1 minute
```

### Journey Planner — With Interchange
```
Source: Dwarka Sector 8 → Destination: Dhansa Bus Stand | Time: 07:30

Journey Plan:
Start at Dwarka Sector 8 (Blue Line)
Next metro at 07:32
Route: Dwarka Sector 8 → Dwarka (Delhi) → Dhansa Bus Stand
Arrive at Dwarka (Delhi) at 07:50  [Transfer to Grey Line]
Next Grey metro departs at 07:53
Arrive at Dhansa Bus Stand at 08:11
Total travel time: 41 minutes
```

---

## 🛠️ How It's Built

### Core Algorithm — Graph-Based Pathfinding
Each station maintains a **neighbor list** of all connected stations. The pathfinder uses a **BFS-like traversal** that:
- Travels in **both directions** on each line
- Handles **branch lines** (Blue Line splits at Yamuna Bank)
- Tracks visited nodes to prevent infinite loops
- Compares all possible interchange routes and returns the fastest

### Data File — `metro_data.txt`
Real DMRC data in CSV format:
```
Line, Station, NextStation, TravelTime(min), Interchange Point
Blue, Rajiv Chowk, Barakhamba Road, 1, No
Blue, Dwarka (Delhi), Dwarka Mor, 2, Yes
Grey, Dwarka (Delhi), Nangli, 2, Yes
...
```

### Key Functions

| Function | Purpose |
|----------|---------|
| `load_metro_data()` | Reads and parses `metro_data.txt` |
| `is_peak_hour()` | Checks if time falls in peak hours |
| `get_next_metro_times()` | Calculates next arrivals at a station |
| `find_path_on_line()` | BFS pathfinding within a single line |
| `find_interchange_stations()` | Finds transfer points between two lines |
| `calculate_travel_time()` | Computes total journey time with delays |
| `plan_journey()` | Master function — orchestrates the full route plan |

---

## 📦 Project Structure

```
sem1-metro-python/
├── metro_simulator.py    ← Main Python program
├── metro_data.txt        ← Real DMRC station & timing data
├── README.md             ← This file
└── ENHANCEMENTS.md       ← Full list of features added
```

---

## 🚀 How to Run

```bash
# Make sure metro_data.txt is in the same folder
python3 metro_simulator.py
```

No pip installs needed — **zero external dependencies.** Pure Python only.

---

## ⚠️ Known Limitations

- Only covers Blue, Magenta, and Grey lines (Yellow, Green, Red not yet added)
- Metro timing has minor calculation variance vs real DMRC schedule
- OTP verification was not part of this project scope
- Fare calculation not implemented yet

---

## 🔮 Future Enhancements

- [ ] Add Yellow, Green, Red, and Violet lines
- [ ] Fare calculation based on distance/zones
- [ ] Peak/off-peak fare difference
- [ ] Multiple route options shown side by side
- [ ] Station facilities and accessibility info

---

## 💡 Key Takeaway

> Built with **zero external libraries** — file I/O, graph traversal, and time logic  
> all implemented from scratch in Python. A 1st semester project that covers  
> data structures, algorithms, and real-world problem solving. 🎓
