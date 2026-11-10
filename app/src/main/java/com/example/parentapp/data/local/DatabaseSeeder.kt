package com.example.parentapp.data.local

import com.example.parentapp.core.security.PasswordHasher
import com.example.parentapp.data.local.entity.Activity
import com.example.parentapp.data.local.entity.CaregiverTask
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.Institution
import com.example.parentapp.data.local.entity.ReferralContact
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.data.local.entity.Tip
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReferralCategory
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.TipCategory
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Populates the database with a demo dataset the first time the app runs (empty DB only).
 * Shared password for every seeded account: **Password123!**
 */
object DatabaseSeeder {

    private const val DEMO_PASSWORD = "Password123!"

    private const val DAY = 86_400_000L
    private const val HOUR = 3_600_000L

    suspend fun seedIfEmpty(db: AppDatabase) = withContext(Dispatchers.IO) {
        if (db.userDao().count() > 0) return@withContext

        val now = System.currentTimeMillis()
        val hash = PasswordHasher.hash(DEMO_PASSWORD)

        // ---- Institutions ----
        val institutionId = db.institutionDao().insertInstitution(
            Institution(
                name = "Bright Future Children's Home",
                type = "Children's home",
                location = "Kampala, Uganda",
                contactPerson = "Grace Namutebi",
                phone = "+256772111222",
            ),
        )

        // ---- Users ----
        val parentId = db.userDao().insertUser(
            UserAccount(
                fullName = "Sarah Mukasa",
                email = "parent@example.com",
                phone = "+256772000001",
                passwordHash = hash,
                role = UserRole.PARENT_GUARDIAN,
                userCategory = "Parent / Guardian",
            ),
        )
        val institutionUserId = db.userDao().insertUser(
            UserAccount(
                fullName = "David Okello",
                email = "institution@example.com",
                phone = "+256772000002",
                passwordHash = hash,
                role = UserRole.INSTITUTIONAL_USER,
                userCategory = "Institution staff",
                institutionId = institutionId,
            ),
        )
        val caregiverId = db.userDao().insertUser(
            UserAccount(
                fullName = "Mary Nabirye",
                email = "caregiver@example.com",
                phone = "+256772000003",
                passwordHash = hash,
                role = UserRole.CAREGIVER,
                userCategory = "Professional caregiver",
            ),
        )
        db.userDao().insertUser(
            UserAccount(
                fullName = "System Administrator",
                email = "admin@example.com",
                phone = "+256772000004",
                passwordHash = hash,
                role = UserRole.ADMIN,
                userCategory = "Administrator",
            ),
        )
        val caregiver2Id = db.userDao().insertUser(
            UserAccount(
                fullName = "Peter Wasswa",
                email = "caregiver2@example.com",
                phone = "+256772000005",
                passwordHash = hash,
                role = UserRole.CAREGIVER,
                userCategory = "Professional caregiver",
            ),
        )

        // ---- Children ----
        val amaniId = db.childDao().insertChild(
            ChildProfile(
                fullName = "Amani Mukasa",
                gender = "Female",
                dateOfBirth = "2018-04-12",
                guardianName = "Sarah Mukasa",
                guardianContact = "+256772000001",
                householdInstitution = "Family",
                assignedCaregiver = "Mary Nabirye",
                assignedCaregiverId = caregiverId,
                schoolName = "Nakasero Primary School",
                medicalNotes = "Mild peanut allergy. Carries antihistamine.",
                careNotes = "Enjoys drawing and story time.",
                createdByUserId = parentId,
            ),
        )
        val brianId = db.childDao().insertChild(
            ChildProfile(
                fullName = "Brian Mukasa",
                gender = "Male",
                dateOfBirth = "2020-09-30",
                guardianName = "Sarah Mukasa",
                guardianContact = "+256772000001",
                householdInstitution = "Family",
                assignedCaregiver = "Mary Nabirye",
                assignedCaregiverId = caregiverId,
                schoolName = "Little Steps Daycare",
                medicalNotes = "Up to date on immunisations.",
                careNotes = "Afternoon nap 1:00–2:30 pm.",
                createdByUserId = parentId,
            ),
        )
        val zawadiId = db.childDao().insertChild(
            ChildProfile(
                fullName = "Zawadi Okello",
                gender = "Female",
                dateOfBirth = "2016-01-20",
                guardianName = "Bright Future Children's Home",
                guardianContact = "+256772111222",
                householdInstitution = "Bright Future Children's Home",
                assignedCaregiver = "Peter Wasswa",
                assignedCaregiverId = caregiver2Id,
                schoolName = "Bright Future Academy",
                medicalNotes = "Asthma — blue inhaler as needed.",
                careNotes = "Responds well to routine and clear instructions.",
                institutionId = institutionId,
                createdByUserId = institutionUserId,
            ),
        )

        // ---- Activities ----
        val activities = listOf(
            Activity(childId = amaniId, category = ActivityCategory.SCHOOL, title = "Parent–teacher meeting", description = "Termly progress review with class teacher.", activityDateTime = now - 2 * DAY, status = ActivityStatus.COMPLETED, createdByUserId = parentId),
            Activity(childId = amaniId, category = ActivityCategory.HEALTH, title = "Dental check-up", description = "Routine six-month dental visit.", activityDateTime = now + 3 * DAY, status = ActivityStatus.PENDING, createdByUserId = parentId),
            Activity(childId = amaniId, category = ActivityCategory.MEDICATION, title = "Antihistamine refill", description = "Collect prescription from pharmacy.", activityDateTime = now - 5 * HOUR, status = ActivityStatus.COMPLETED, createdByUserId = parentId),
            Activity(childId = amaniId, category = ActivityCategory.PLAY, title = "Art class", description = "Saturday community art session.", activityDateTime = now + 2 * DAY, status = ActivityStatus.PENDING, createdByUserId = parentId),
            Activity(childId = brianId, category = ActivityCategory.FEEDING, title = "Lunch — balanced plate", description = "Rice, greens and beans. Ate well.", activityDateTime = now - 6 * HOUR, status = ActivityStatus.COMPLETED, createdByUserId = caregiverId),
            Activity(childId = brianId, category = ActivityCategory.HEALTH, title = "Growth monitoring", description = "Weigh-in at clinic.", activityDateTime = now + 5 * DAY, status = ActivityStatus.PENDING, createdByUserId = parentId),
            Activity(childId = brianId, category = ActivityCategory.PLAY, title = "Outdoor play", description = "Ball games at the park, 45 minutes.", activityDateTime = now - DAY, status = ActivityStatus.COMPLETED, createdByUserId = caregiverId),
            Activity(childId = brianId, category = ActivityCategory.SCHOOL, title = "Daycare drop-off", description = "Morning drop-off.", activityDateTime = now - 30 * HOUR, status = ActivityStatus.COMPLETED, createdByUserId = parentId),
            Activity(childId = zawadiId, category = ActivityCategory.MEDICATION, title = "Inhaler technique review", description = "Nurse to review spacer technique.", activityDateTime = now + DAY, status = ActivityStatus.PENDING, createdByUserId = institutionUserId),
            Activity(childId = zawadiId, category = ActivityCategory.SCHOOL, title = "Homework club", description = "After-school reading support.", activityDateTime = now - 3 * HOUR, status = ActivityStatus.COMPLETED, createdByUserId = caregiver2Id),
            Activity(childId = zawadiId, category = ActivityCategory.HEALTH, title = "Asthma review", description = "Quarterly asthma review at health centre.", activityDateTime = now + 8 * DAY, status = ActivityStatus.PENDING, createdByUserId = institutionUserId),
            Activity(childId = zawadiId, category = ActivityCategory.FEEDING, title = "Breakfast", description = "Porridge and fruit.", activityDateTime = now - 28 * HOUR, status = ActivityStatus.COMPLETED, createdByUserId = caregiver2Id),
        )
        activities.forEach { db.activityDao().insertActivity(it) }

        // ---- Reminders ----
        val reminders = listOf(
            Reminder(childId = amaniId, title = "Give antihistamine", category = ReminderCategory.MEDICATION, reminderDateTime = now + 2 * HOUR, leadTimeMinutes = 30, priority = Priority.HIGH, status = ReminderStatus.PENDING, notificationEnabled = true, createdByUserId = parentId),
            Reminder(childId = amaniId, title = "Dental check-up", category = ReminderCategory.APPOINTMENT, reminderDateTime = now + 3 * DAY, leadTimeMinutes = 1_440, priority = Priority.MEDIUM, status = ReminderStatus.PENDING, notificationEnabled = true, createdByUserId = parentId),
            Reminder(childId = amaniId, title = "Pack swimming kit", category = ReminderCategory.SCHOOL, reminderDateTime = now - 2 * DAY, leadTimeMinutes = 60, priority = Priority.LOW, status = ReminderStatus.MISSED, notificationEnabled = true, createdByUserId = parentId),
            Reminder(childId = brianId, title = "Nap time", category = ReminderCategory.PLAY, reminderDateTime = now + 4 * HOUR, leadTimeMinutes = 10, priority = Priority.LOW, status = ReminderStatus.PENDING, notificationEnabled = true, createdByUserId = caregiverId),
            Reminder(childId = brianId, title = "Clinic weigh-in", category = ReminderCategory.HEALTH, reminderDateTime = now + 5 * DAY, leadTimeMinutes = 180, priority = Priority.MEDIUM, status = ReminderStatus.PENDING, notificationEnabled = true, createdByUserId = parentId),
            Reminder(childId = brianId, title = "Immunisation follow-up", category = ReminderCategory.HEALTH, reminderDateTime = now - 6 * DAY, leadTimeMinutes = 1_440, priority = Priority.HIGH, status = ReminderStatus.COMPLETED, notificationEnabled = true, createdByUserId = parentId),
            Reminder(childId = zawadiId, title = "Refill blue inhaler", category = ReminderCategory.MEDICATION, reminderDateTime = now + 12 * HOUR, leadTimeMinutes = 60, priority = Priority.URGENT, status = ReminderStatus.PENDING, notificationEnabled = true, createdByUserId = caregiver2Id),
            Reminder(childId = zawadiId, title = "Asthma review booking", category = ReminderCategory.APPOINTMENT, reminderDateTime = now - 30 * HOUR, leadTimeMinutes = 1_440, priority = Priority.HIGH, status = ReminderStatus.MISSED, notificationEnabled = true, createdByUserId = institutionUserId),
        )
        reminders.forEach { db.reminderDao().insertReminder(it) }

        // ---- Caregiver tasks ----
        val tasks = listOf(
            CaregiverTask(childId = amaniId, assignedToCaregiverId = caregiverId, assignedByUserId = parentId, title = "School pick-up at 3:30 pm", description = "Collect Amani from Nakasero Primary and walk home.", dueDateTime = now + 6 * HOUR, status = TaskStatus.PENDING),
            CaregiverTask(childId = amaniId, assignedToCaregiverId = caregiverId, assignedByUserId = parentId, title = "Prepare afternoon snack", description = "Fruit and yoghurt. No nuts.", dueDateTime = now - 3 * HOUR, status = TaskStatus.COMPLETED, completedAt = now - 2 * HOUR),
            CaregiverTask(childId = brianId, assignedToCaregiverId = caregiverId, assignedByUserId = parentId, title = "Supervise nap", description = "Nap 1:00–2:30 pm, dim the room.", dueDateTime = now + 3 * HOUR, status = TaskStatus.PENDING),
            CaregiverTask(childId = brianId, assignedToCaregiverId = caregiverId, assignedByUserId = parentId, title = "Log lunch intake", description = "Record what Brian eats at lunch.", dueDateTime = now - 26 * HOUR, status = TaskStatus.MISSED),
            CaregiverTask(childId = zawadiId, assignedToCaregiverId = caregiver2Id, assignedByUserId = institutionUserId, title = "Check inhaler before school", description = "Confirm inhaler is in the school bag each morning.", dueDateTime = now + 20 * HOUR, status = TaskStatus.PENDING),
            CaregiverTask(childId = zawadiId, assignedToCaregiverId = caregiver2Id, assignedByUserId = institutionUserId, title = "Homework club sign-in", description = "Sign Zawadi in and out of homework club.", dueDateTime = now - 4 * HOUR, status = TaskStatus.REQUIRES_FOLLOW_UP, followUpNotes = "Zawadi left early — please confirm reason with school."),
        )
        tasks.forEach { db.caregiverTaskDao().insertTask(it) }

        seedTips(db)
        seedReferrals(db)
    }

    private suspend fun seedTips(db: AppDatabase) {
        val tips = listOf(
            Tip(title = "Responsive play builds language", category = TipCategory.DEVELOPMENT, summary = "Follow the child's lead during play.", content = "Get down to the child's level, name what they are doing and wait for a response. This back-and-forth ('serve and return') is how young children build vocabulary and confidence."),
            Tip(title = "Screen time under 5", category = TipCategory.DEVELOPMENT, summary = "Keep it short, shared and age-appropriate.", content = "For children aged 2–5, limit recreational screen time to about an hour a day of good-quality content, ideally watched together so you can talk about it. Avoid screens in the hour before bed."),
            Tip(title = "The 20-second handwash", category = TipCategory.HYGIENE, summary = "Soap, water and a song.", content = "Wet hands, apply soap, and rub all surfaces — backs of hands, between fingers and under nails — for 20 seconds before rinsing. Singing a short song helps children keep going long enough."),
            Tip(title = "Daily tooth brushing", category = TipCategory.HYGIENE, summary = "Twice a day with a pea-sized amount of fluoride toothpaste.", content = "Brush last thing at night and one other time each day. Help or supervise brushing until at least age 7, and encourage spitting rather than rinsing so fluoride keeps working."),
            Tip(title = "Build a balanced plate", category = TipCategory.NUTRITION, summary = "Half vegetables and fruit, a quarter grains, a quarter protein.", content = "Offer a variety of colours across the week, choose whole grains where you can, and serve water or milk rather than sugary drinks. Let children decide how much of what you serve they eat."),
            Tip(title = "Introducing new foods", category = TipCategory.NUTRITION, summary = "It can take many tries.", content = "A child may need to see and taste a new food 8–15 times before accepting it. Keep portions tiny, stay relaxed, and eat the same food yourself — pressure tends to backfire."),
            Tip(title = "Safe sleep for infants", category = TipCategory.SAFETY, summary = "Back to sleep, clear cot.", content = "Place babies on their back to sleep in a cot with a firm flat mattress and no pillows, bumpers or loose bedding. Keep the room a comfortable temperature and smoke-free."),
            Tip(title = "Home poison safety", category = TipCategory.SAFETY, summary = "Lock medicines and cleaners away.", content = "Store medicines, cleaning products and paraffin in their original containers, up high or locked away. Save the poison helpline number in your phone and act fast if you suspect a swallow."),
            Tip(title = "Road safety for young children", category = TipCategory.SAFETY, summary = "Hold hands and model good habits.", content = "Always hold a young child's hand near traffic, cross at designated points, and narrate what you are doing ('we stop, we look both ways'). Children under about 10 cannot reliably judge traffic speed."),
            Tip(title = "Water safety", category = TipCategory.SAFETY, summary = "Constant, close supervision.", content = "Stay within arm's reach of young children in or near water — baths, buckets, pools and open water. Empty containers after use and never rely on flotation toys as safety devices."),
            Tip(title = "Spotting a fever", category = TipCategory.HEALTH, summary = "Know when to seek care.", content = "A temperature at or above 38°C is a fever. Seek medical care urgently for any fever in a baby under 3 months, a stiff neck, a rash that does not fade under pressure, breathing difficulty, or a child who is unusually drowsy."),
            Tip(title = "Keep vaccinations on schedule", category = TipCategory.HEALTH, summary = "Use the child health card.", content = "Bring the child's health/immunisation card to every visit and keep it safe. If a dose is missed, the clinic can usually catch up — you rarely need to start a course again."),
            Tip(title = "Managing tantrums calmly", category = TipCategory.BEHAVIOUR, summary = "Stay close, stay calm, keep them safe.", content = "Tantrums are normal for toddlers who cannot yet manage big feelings. Acknowledge the feeling, keep the child safe, and wait it out without giving in to the demand. Reconnect warmly afterwards."),
            Tip(title = "Praise the effort", category = TipCategory.BEHAVIOUR, summary = "Be specific about what they did.", content = "'You kept trying until you finished the puzzle' teaches more than 'you're so clever'. Describing effort and strategy helps children persist with hard tasks."),
            Tip(title = "Consistent routines help behaviour", category = TipCategory.BEHAVIOUR, summary = "Predictable days feel safe.", content = "Regular times for meals, play, wind-down and sleep reduce power struggles. A short picture schedule on the wall helps young children know what comes next."),
        )
        tips.forEach { db.tipDao().insertTip(it) }
    }

    private suspend fun seedReferrals(db: AppDatabase) {
        val contacts = listOf(
            ReferralContact(name = "Uganda Child Helpline — Sauti 116", category = ReferralCategory.EMERGENCY, phone = "116", location = "National, Uganda", description = "Free 24/7 helpline run by the Ministry of Gender, Labour and Social Development for reporting child abuse, neglect and children in danger. Toll-free from any network.", isEmergency = true, isHotline = true),
            ReferralContact(name = "Police emergency", category = ReferralCategory.EMERGENCY, phone = "999", location = "National, Uganda", description = "Uganda Police emergency line (also 112 from a mobile phone).", isEmergency = true, isHotline = true),
            ReferralContact(name = "GBV toll-free helpline", category = ReferralCategory.EMERGENCY, phone = "0800333333", location = "National, Uganda", description = "Ministry of Gender toll-free line for gender-based violence and violence against children — advice, referral and follow-up.", isEmergency = true, isHotline = true),
            ReferralContact(name = "Uganda Red Cross Society", category = ReferralCategory.EMERGENCY, phone = "+256312265316", location = "Headquarters, Kampala (branches nationwide)", description = "Ambulance, first aid and disaster response. Call your nearest district branch where possible.", isEmergency = true, isHotline = true),
            ReferralContact(name = "Probation & Social Welfare Officer (PSWO)", category = ReferralCategory.CHILD_PROTECTION, phone = "", location = "District headquarters", description = "The government officer responsible for child protection, alternative family care, custody, resettlement and welfare. Ask for the PSWO at your district or municipal offices. Save their direct number here.", isEmergency = false),
            ReferralContact(name = "Police Child & Family Protection Unit", category = ReferralCategory.CHILD_PROTECTION, phone = "999", location = "District and division police stations", description = "Handles child abuse, defilement, neglect, child stealing and child maintenance. Report at the nearest station and ask for the Child and Family Protection Unit (CFPU).", isEmergency = false),
            ReferralContact(name = "Nearest regional referral hospital", category = ReferralCategory.HEALTH, phone = "", location = "Regional referral hospital", description = "For serious illness or injury, go to the casualty department of your regional referral hospital (e.g. Mulago, Mbarara, Gulu, Mbale, Jinja). Save the direct number for your facility here.", isEmergency = false),
            ReferralContact(name = "Nearest Health Centre III / IV", category = ReferralCategory.HEALTH, phone = "", location = "Sub-county / county", description = "Routine immunisation, growth monitoring, minor illness, antenatal and young child clinics.", isEmergency = false),
            ReferralContact(name = "FIDA Uganda (legal aid)", category = ReferralCategory.LEGAL, phone = "+256414530848", location = "Kampala, with offices in Mbarara, Gulu, Mbale and Arua", description = "Uganda Association of Women Lawyers — free legal aid and advice for women and children, including custody, maintenance and protection orders.", isEmergency = false),
            ReferralContact(name = "Justice Centres Uganda", category = ReferralCategory.LEGAL, phone = "0800100210", location = "National, Uganda", description = "Government toll-free legal aid line for people who cannot afford a lawyer — family, child and land matters.", isEmergency = false),
            ReferralContact(name = "District Education Officer (DEO)", category = ReferralCategory.EDUCATION, phone = "", location = "District headquarters", description = "School placement, transfers, UPE/USE enrolment, and support for out-of-school children.", isEmergency = false),
            ReferralContact(name = "TPO Uganda (psychosocial support)", category = ReferralCategory.COUNSELLING, phone = "+256414289324", location = "Kampala, with field offices in several districts", description = "Counselling and mental-health support for children, parents and caregivers under strain, including after violence or loss.", isEmergency = false),
            ReferralContact(name = "District Community Development Office", category = ReferralCategory.SOCIAL_WELFARE, phone = "", location = "District / municipal headquarters", description = "Information on support for orphans and vulnerable children (OVC), community-based care and social protection programmes.", isEmergency = false),
        )
        contacts.forEach { db.referralContactDao().insertContact(it) }
    }
}
