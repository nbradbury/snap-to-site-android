package com.nbradbury.snaptosite

import com.nbradbury.snaptosite.ai.About
import com.nbradbury.snaptosite.ai.Contact
import com.nbradbury.snaptosite.ai.DayHours
import com.nbradbury.snaptosite.ai.Event
import com.nbradbury.snaptosite.ai.Hero
import com.nbradbury.snaptosite.ai.MenuCategory
import com.nbradbury.snaptosite.ai.MenuItem
import com.nbradbury.snaptosite.ai.SiteModel
import com.nbradbury.snaptosite.ai.Special

val cafe = SiteModel(
    businessName = "Juniper & Rye",
    tagline = "Espresso, pastries and soup on Alder Street",
    accentColor = "#2e4b3c",
    logoPhoto = 0,
    banner = null,
    hero = Hero("Espresso, morning pastries & soup at lunch.", photo = 1),
    about = About("A neighborhood café at 418 Alder Street.\n\nEverything is baked here each morning.", photo = null),
    special = Special("Today's soup", "Ask at the counter"),
    menu = listOf(
        MenuCategory("Coffee", listOf(MenuItem("Latte", "\$4.75", null), MenuItem("Espresso", "\$3.25", null))),
    ),
    hours = listOf(DayHours("Monday", "Closed", false), DayHours("Tuesday – Sunday", "7am – 3pm", false)),
    events = listOf(Event("Open mic night", "2026-11-06", "6 – 8pm", "Bring a song", photo = 2)),
    contact = Contact(address = "418 Alder Street", phone = "(555) 123-4567", email = null),
)
