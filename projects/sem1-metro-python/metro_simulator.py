starting_time   = "06:00"
ending_time     = "23:00"
peak_time       = [(8,10), (17,19)]     # morning + evening rush
off_peak_freq   = 8                     # mins between metros during off-peak
peak_freq       = 4
interchange_time = 3                    # extra wait during line changes

# load_data() : reads metro_data.txt and builds structure

def load_data(fname):

    metro = {}        # this dict will store all lines + stations
    try:
        f = open(fname, 'r')
        lines = f.readlines()
        f.close()

        # skip the header which is in 
        for line in lines[1:]:
            line = line.strip()
            if not line:
                continue
            parts = line.split(',')
            
            if len(parts) >= 5:

                line_name = parts[0].strip()
                station   = parts[1].strip()
                nxt       = parts[2].strip()
                t_time    = int(parts[3].strip())
                is_inter  = (parts[4].strip() == "Yes")

                if line_name not in metro:
                    metro[line_name] = {
                        'stations':[],
                        'connect':{},
                        'travel_times':{},
                        'interchange_stations':[]
                    }

                # in this we are adding station names
                if station not in metro[line_name]['stations']:
                    metro[line_name]['stations'].append(station)

                if nxt not in metro[line_name]['stations']:
                    metro[line_name]['stations'].append(nxt)

                # we are adding connections both ways
                c = metro[line_name]['connect']
                if station not in c:  
                    c[station] = []
                if nxt not in c:  
                    c[nxt] = []

                if nxt not in c[station]:
                    c[station].append(nxt)
                if station not in c[nxt]:
                    c[nxt].append(station)

                # we are saving travel times
                metro[line_name]['travel_times'][(station,nxt)] = t_time
                metro[line_name]['travel_times'][(nxt,station)] = t_time

                # mark interchange stations
                if is_inter:
                    if station not in metro[line_name]['interchange_stations']:
                        metro[line_name]['interchange_stations'].append(station)

                    if nxt not in metro[line_name]['interchange_stations']:
                        metro[line_name]['interchange_stations'].append(nxt)

        return metro

    except FileNotFoundError:
        print("File not found:", fname)
        return {}
    except Exception as e:
        print("Error:", e)
        return {}

# t2min() : convert HH:MM to minutes

def t2min(t):
    try:
        h, m = t.split(':')
        return int(h)*60 + int(m)
    except:
        return -1

# min2t() : convert minutes back to HH:MM

def min2t(m):
    h  = m//60
    mm = m%60
    return f"{h:02d}:{mm:02d}"

# is_peak() : checks if current time is within rush hour

def is_peak(h, m):
    tot = h*60 + m
    for s,e in peak_time:
        if s*60 <= tot < e*60:
            return True
    return False

# get_freq() : returns train frequency for given time

def get_freq(h, m):
    return peak_freq if is_peak(h,m) else off_peak_freq

# service_ok() : checks if metro service is open

def service_ok(t):
    tm = t2min(t)
    start = t2min(starting_time)
    end   = t2min(ending_time)
    return start <= tm <= end

# next_metro() : calculates next & subsequent metro timings

def next_metro(st, line, now, metro):

    if not service_ok(now):    
        return None
    if line not in metro:      
        return None
    if st not in metro[line]['stations']:  
        return None

    cur   = t2min(now)
    start = t2min(starting_time)
    diff  = cur - start

    hh, mm = map(int, now.split(':'))
    freq = get_freq(hh, mm)

    mins_since_last = diff % freq
    wait = 0 if mins_since_last == 0 else (freq - mins_since_last)

    nextm = cur + wait
    end   = t2min(ending_time)

    if nextm > end:
        return None

    nxt_time = min2t(nextm)

    subs = []
    t2 = nextm + freq
    while t2 <= end and len(subs) < 5:
        subs.append(min2t(t2))
        t2 += freq

    return {"next":nxt_time, "subsequent":subs}

# find_lines() : returns which lines contain this station

def find_lines(st, metro):
    ans = []
    st  = st.strip()

    for ln, data in metro.items():
        if st in data["stations"]:
            ans.append(ln)

    if ans:
        return ans

    # lowercase match
    st_low = st.lower()
    for ln,data in metro.items():
        for s in data["stations"]:
            if s.lower() == st_low:
                ans.append(ln)
                break

    return ans

# path_line() : BFS path along the same metro line

def path_line(src, dest, line, metro):

    if line not in metro:
        return None

    sts = metro[line]["stations"]
    con = metro[line]["connect"]

    if src not in sts or dest not in sts:
        return None

    if src == dest:
        return [src]

    q   = [[src]]
    vis = set([src])

    while q:
        p   = q.pop(0)
        cur = p[-1]

        for ne in con.get(cur,[]):
            if ne in vis:
                continue
            np = p + [ne]
            if ne == dest:
                return np

            vis.add(ne)
            q.append(np)

    return None

# inters() : finds interchange stations between 2 lines

def inters(l1, l2, metro):

    if l1 not in metro or l2 not in metro:
        return []

    s1 = set(metro[l1]['stations'])
    s2 = set(metro[l2]['stations'])
    common = s1.intersection(s2)

    out = []
    for st in common:
        if (st in metro[l1]['interchange_stations']) or (st in metro[l2]['interchange_stations']):
            out.append(st)

    return out

# ttime() : total travel time along a path on one line

def ttime(path, line, metro):

    if line not in metro:
        return 0

    tt  = metro[line]['travel_times']
    tot = 0

    for i in range(len(path)-1):
        a = path[i]
        b = path[i+1]

        if (a,b) in tt:
            tot += tt[(a,b)]

    return tot

# plan() : the journey planning engine (direct & interchanges)

def plan(src, dest, t, metro):

    if not service_ok(t):
        return None

    slines = find_lines(src, metro)
    dlines = find_lines(dest, metro)

    if not slines or not dlines:
        return None


    # direct same-line route 
    common = set(slines).intersection(dlines)

    if common:
        ln = list(common)[0]
        p  = path_line(src, dest, ln, metro)

        if p:
            info = next_metro(src, ln, t, metro)
            if not info:
                return None

            dep = t2min(info['next'])
            tr  = ttime(p, ln, metro)
            arr = min2t(dep + tr)

            return {
                "type": "direct",
                "source_line": ln,
                "path": p,
                "next_metro": info['next'],
                "arrival_time": arr,
                "total_time": tr,
                "interchange": None
            }


    # single interchange search
    best = None
    best_t = float("inf")

    for sL in slines:
        for dL in dlines:

            if sL == dL:
                continue

            ints = inters(sL, dL, metro)

            for ic in ints:

                p1 = path_line(src, ic, sL, metro)
                if not p1: continue

                p2 = path_line(ic, dest, dL, metro)
                if not p2: continue

                t1 = ttime(p1, sL, metro)
                t2 = ttime(p2, dL, metro)

                info1 = next_metro(src, sL, t, metro)
                if not info1:   continue

                dep1  = t2min(info1['next'])
                arrIC = dep1 + t1

                req2 = min2t(arrIC + interchange_time)
                info2 = next_metro(ic, dL, req2, metro)
                if not info2:   continue

                dep2 = t2min(info2['next'])

                if dep2 < arrIC + interchange_time:
                    if info2['subsequent']:
                        dep2 = t2min(info2['subsequent'][0])
                    else:
                        continue

                arr_final = dep2 + t2
                total = arr_final - dep1

                if total < best_t:
                    best_t = total

                    best = {
                        "type":"interchange",
                        "source_line":sL,
                        "dest_line":dL,
                        "path1":p1,
                        "path2":p2,
                        "interchange_station":ic,
                        "next_metro":info1['next'],
                        "arrival_at_interchange":min2t(arrIC),
                        "interchange_departure":min2t(dep2),
                        "arrival_time":min2t(arr_final),
                        "total_time":total
                    }

    if best:
        return best


    # double interchange  (rare case) 
    dbl = None
    dbl_t = float("inf")
    allL = list(metro.keys())

    for sL in slines:
        for mid in allL:

            if mid == sL:   continue

            for dL in dlines:
                if dL in (sL, mid):
                    continue

                ints1 = inters(sL, mid, metro)
                ints2 = inters(mid, dL, metro)

                for ic1 in ints1:

                    p1 = path_line(src, ic1, sL, metro)
                    if not p1: continue

                    t1 = ttime(p1, sL, metro)

                    for ic2 in ints2:

                        p2 = path_line(ic1, ic2, mid, metro)
                        p3 = path_line(ic2, dest, dL, metro)
                        if not p2 or not p3:
                            continue

                        t2 = ttime(p2, mid, metro)
                        t3 = ttime(p3, dL, metro)

                        info1 = next_metro(src, sL, t, metro)
                        if not info1: continue

                        dep1 = t2min(info1['next'])
                        arr1 = dep1 + t1

                        req2 = min2t(arr1 + interchange_time)
                        info2 = next_metro(ic1, mid, req2, metro)
                        if not info2: continue

                        dep2 = t2min(info2['next'])
                        if dep2 < arr1 + interchange_time:
                            if info2['subsequent']:
                                dep2 = t2min(info2['subsequent'][0])
                            else:
                                continue

                        arr2 = dep2 + t2

                        req3 = min2t(arr2 + interchange_time)
                        info3 = next_metro(ic2, dL, req3, metro)
                        if not info3: continue

                        dep3 = t2min(info3['next'])
                        if dep3 < arr2 + interchange_time:
                            if info3['subsequent']:
                                dep3 = t2min(info3['subsequent'][0])
                            else:
                                continue

                        arr_final = dep3 + t3
                        total = arr_final - dep1

                        if total < dbl_t:

                            dbl_t = total
                            dbl = {
                                "type":"double_interchange",
                                "source_line":sL,
                                "mid_line":mid,
                                "dest_line":dL,
                                "path1":p1,
                                "path2":p2,
                                "path3":p3,
                                "interchange_station1":ic1,
                                "interchange_station2":ic2,
                                "next_metro":min2t(dep1),
                                "arrival_at_interchange1":min2t(arr1),
                                "mid_departure":min2t(dep2),
                                "arrival_at_interchange2":min2t(arr2),
                                "interchange_departure":min2t(dep3),
                                "arrival_time":min2t(arr_final),
                                "total_time":total
                            }

    return dbl

# show_line() : prints all stations of a given line

def show_line(ln, metro):

    if ln not in metro:
        print("Line not found:", ln)
        return

    print("\n", ln, "Line Stations")
    # print("-"*40)

    for i, st in enumerate(metro[ln]["stations"], 1):

        if st in metro[ln]["interchange_stations"]:
            print(f"{i}. {st}   [Interchange]")
        else:
            print(f"{i}. {st}")

    # print("-"*40)

# show_all() : displays all stations across lines neatly

def show_all(metro):
    print("\nAll Available Stations")
    # print("="*60)

    for ln in ["Blue","Magenta","Grey"]:
        if ln in metro:

            sts = metro[ln]["stations"]
            print(f"\n{ln} Line ({len(sts)} stations)")
            # print("-"*60)

            for i in range(0, len(sts), 3):
                row = []
                for j in range(3):
                    idx = i + j
                    if idx < len(sts):
                        s = sts[idx]
                        if s in metro[ln]['interchange_stations']:
                            row.append(f"{idx+1}. {s} [I]".ljust(28))
                        else:
                            row.append(f"{idx+1}. {s}".ljust(28))
                print("  ".join(row))

    # print("="*60)

# clean_station() : tries to sanitize messy user input

def clean_station(txt, metro):

    txt = txt.strip()

    # remove any "1. station"
    if '.' in txt and txt[0].isdigit():
        txt = txt.split('.',1)[1].strip()

    if txt.endswith('[I]'):            txt = txt[:-3].strip()
    if txt.endswith('[Interchange]'):  txt = txt[:-13].strip()

    for ln,d in metro.items():
        if txt in d["stations"]:
            return txt

    low = txt.lower()
    for ln,d in metro.items():
        for s in d["stations"]:
            if s.lower() == low:
                return s

    for ln,d in metro.items():
        for s in d["stations"]:
            if low in s.lower():
                return s

    return None

# ui_time() : handles UI for next metro timing

def ui_time():

    print("\nMetro Timings")

    metro = load_data("metro_data.txt")
    lines = [ln for ln in ["Blue","Magenta","Grey"] if ln in metro]

    if not lines:
        lines = list(metro.keys())

    print("\nAvailable:", ", ".join(lines))

    ln = input("Enter Line: ").strip().capitalize()
    if ln not in metro or ln not in lines:
        print("Invalid line")
        return

    show_line(ln, metro)

    st  = input("Enter station: ").strip()
    now = input("Enter current time (HH:MM): ").strip()

    if not service_ok(now):
        print("Metro closed (06:00 - 23:00)")
        return

    res = next_metro(st, ln, now, metro)

    if res:
        print("\nNext metro at:", res['next'])
        if res["subsequent"]:
            print("Next ones:", ", ".join(res["subsequent"]))
    else:
        print("Station not found or wrong input")

# ui_plan() : handles UI for journey planning

def ui_plan():
    metro = load_data("metro_data.txt")
    show_all(metro)

    s1 = input("Source: ").strip()
    s2 = input("Destination: ").strip()
    t  = input("Travel time (HH:MM): ").strip()

    src  = clean_station(s1, metro)
    dest = clean_station(s2, metro)

    if not src:
        print("Invalid source")
        return

    if not dest:
        print("Invalid destination")
        return

    if not service_ok(t):
        print("Metro not running at that time")
        return

    ans = plan(src, dest, t, metro)

    if not ans:
        print("No route found.")
        return

    print("\nJourney Plan")
    print("Start:", src, "(", ans['source_line'], ")")
    print("Next Metro:", ans['next_metro'])

    if ans["type"] == "direct":

        print("Direct Route")
        print("Arrive:", ans['arrival_time'])
        print("Travel:", ans['total_time'], "mins")


    elif ans["type"] == "interchange":

        print("Route:", src, "->", ans["interchange_station"], "->", dest)
        print("Arrive at interchange:", ans["arrival_at_interchange"])
        print("Change to", ans["dest_line"])
        print("Depart:", ans['interchange_departure'])
        print("Arrive final:", ans['arrival_time'])
        print("Travel:", ans['total_time'], "mins")


    else:       # double interchange

        print("Route involves 2 interchanges!")
        print(src, "->", ans["interchange_station1"], "->", ans["interchange_station2"], "->", dest)
        print("Arrive 1:", ans['arrival_at_interchange1'])
        print("Mid depart:", ans['mid_departure'])
        print("Arrive 2:", ans['arrival_at_interchange2'])
        print("Final:", ans['arrival_time'])
        print("Travel:", ans['total_time'], "mins")

# main() : the top-level menu loop for the simulator

def main():

    while True:

        print("\nMain Menu")
        print("1. Metro Timings")
        print("2. Journey Planner")
        print("3. Exit")

        c = input("Choice: ").strip()

        if c == '1':
            ui_time()

        elif c == '2':
            ui_plan()

        elif c == '3':
            print("\nThanks for using the Metro Simulator!")
            break

        else:
            print("Invalid choice.")

main()
