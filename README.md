# air-respeck

App's pages:\
[Supervisor Mode]
1. Live Readings
2. Activity Summary
3. Activity Logging
4. Navigation drawer header
5. Kebab menu
(5a.) Subject mode
5b. Respeck device
(5c.) Check for updates
(5d.) Close app

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

5)
code: activities/MainActivity (line 963)\
xml: menu/menu_supervised.xml

5b) 
code: activities/ConfigViewActivity
xml: activity_view_config.xml

6)
code: fragments/HomeTabFragment\
xml: fragment_subject_home.xml

7)
code: fragments/LiveActTabFragment\
xml: fragment_live_act_tab.xml

8)
code: fragments/ActSumTabFragment\
xml: fragment_act_sum_tab.xml

**(Tab menu inflates these fragments)**\
code: fragments/actsum_today_Fragment\
xml: layout/fragment_sup_actsum_today.xml

code: fragments/actsum_pastweek_Fragment\
xml: layout/fragment_actsum_pastweek_.xml

9)
code: fragments/SettingsTabFragment\
xml: layout/fragment_settings_tab.xml

10)
code: activities/MainActivity (line 963)\
xml: menu/menu_subject.xml

11)
code: fragments/ComfortHomeFragment\
xml: layout/fragment_comfort_home.xml

12)
code: activities/MainActivity (line 963)\
xml:menu/ menu_comfort.xml
