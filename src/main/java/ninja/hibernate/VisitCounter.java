package ninja.hibernate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingLong;

class VisitCounter {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static class UserStats {
        private Optional<Long> visitsCount;

        Optional<Long> getVisitsCount() {
            return visitsCount;
        }

        void setVisitsCount(Optional<Long> visitsCount) {
            this.visitsCount = visitsCount;
        }
    }

    public static void main(String[] args) {
        List<Map<String, UserStats>> listOfMaps = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, UserStats> map = new HashMap<>();
            UserStats userStats = new UserStats();
            Optional<Long> l = Optional.of(Integer.toUnsignedLong(i));
            userStats.setVisitsCount(l);
            map.put(Integer.toString(i), userStats);
            addMap(3, map, listOfMaps);
        }

        System.out.println(VisitCounter.count(listOfMaps));
        /*
            The call to another signature should look like this:
            System.out.println(VisitCounter.count(listOfMaps.get(5)), ...);
         */
    }

    private static <K, V> void addMap(@SuppressWarnings("SameParameterValue") int times, Map<K, V> map, List<Map<K, V>> list) {
        int i = 0;
        while (i < times) {
            list.add(map);
            ++i;
        }
    }

    private static Map<Long, Long> count(List<Map<String, UserStats>> visits) {
        /*
            Initially method signature assumed to look like this

            private static Map<Long, Long> count(Map<String, UserStats>... visits) {

            but that's redundant and useless
         */

        List<Map<String, UserStats>> visitMaps = new ArrayList<>();
        for (Map<String, UserStats> visitMap : visits) {
            if (Objects.nonNull(visitMap) && !visitMap.isEmpty()) {
                visitMaps.add(visitMap);
            } else {
                visitMaps.add(new HashMap<>());
            }
        }
        return visitMaps.stream().map(stringUserStatsMap -> {
                    Set<String> keys = stringUserStatsMap.keySet();
                    keys = keys.stream()
                            .filter(key -> {
                                long parsedValue = Long.parseLong(key);
                                return Long.MIN_VALUE < parsedValue && parsedValue < Long.MAX_VALUE;
                            })
                            .collect(Collectors.toSet());
                    HashMap<String, UserStats> res = new HashMap<>();
                    for (String key : keys) {
                        UserStats currentStats = stringUserStatsMap.get(key);
                        if (Objects.nonNull(currentStats) && currentStats.getVisitsCount().isPresent()) {
                            res.put(key, currentStats);
                        }
                    }
                    return res;
                }).flatMap(el -> el.entrySet().stream()).
                collect(groupingBy(el -> Long.parseLong(el.getKey()),
                        mapping(Map.Entry::getValue,
                                summingLong(el -> el.getVisitsCount().orElse(0L)))));

                /*      Equivalent of Optional::orElse is here:
                                {
                                    Optional<Long> optionalLong = value.getVisitsCount();
                                    long res = 0L;
                                    if (optionalLong.isPresent()) res = optionalLong.get();
                                    return res;
                                }

                        Option number two of collect method. Collection through the Collectors::toMap:

                        collect(Collectors.toMap(el -> Long.parseLong(el.getKey()), el -> {
                            Optional<Long> visitsCount = el.getValue().getVisitsCount();
                            return visitsCount.orElse(0L);
                        }, Long::sum));
                 */

    }
}
