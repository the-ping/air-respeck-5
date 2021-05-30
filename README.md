# air-respeck

App's pages:\
[Supervisor Mode]
1. Live Readings
2. Activity Summary
3. Activity Logging
4. Navigation drawer header
5. Kebab menu settings

[Subject Mode]\
6. Home
7. Live Readings
8. Activity Summary
9. Settings
10. Kebab menu

[Comfort Mode]\
11. Home
12. Kebab menu



1)
code: fragments/SupervisedRESpeckReadingsIcons\
xml: layout/fragment_respeck_respeck_readings_icons.xml

2)
code: fragments/SupervisedActivitySummaryFragment\
xml: layout/fragment_activity_summary.xml\

**(Tab menu inflates these fragments)**\
code: fragments/sup_actsum_todayFragment\
xml: layout/fragment_sup_actsum_today.xml

code: fragments/sup_actsum_pastweekFragment\
xml: layout/fragment_sup_actsum_pastweek.xml

3)
code: fragments/SupervisedActivityLoggingFragment\
xml: layout/fragment_activity_logging_respeck.xml

4)
code: activity/MainActivity (line 303)\
xml: layout/navigation_header.xml

