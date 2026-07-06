package com.example.data

data class RolePlayTask(
    val id: String,
    val title: String,
    val description: String,
    val mode: String // "HR Recruiter" or "IELTS Examiner"
)

object RolePlayData {

    val interviewMasteryList = listOf(
        RolePlayTask(
            id = "IM_1",
            title = "Software Engineer Mock Interview",
            description = "Simulate a technical system design and behavioral screening with dynamic STAR probing.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_2",
            title = "Product Manager Case Assessment",
            description = "Pitch product sense strategies, handle metrics failures, and define user pain points.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_3",
            title = "Data Analyst Technical Interview",
            description = "Discuss complex SQL queries, analytical pipelines, and translating metrics to business stakeholders.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_4",
            title = "UX Designer Case Study Pitch",
            description = "Defend your design choices, prototype stages, and user persona research under questioning.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_5",
            title = "Marketing Manager Strategy Presentation",
            description = "Present a multi-channel campaign layout and defend target acquisition budget allocation.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_6",
            title = "Finance Associate Technical Screening",
            description = "Walk through financial modeling, risk assessments, and compliance evaluation questions.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_7",
            title = "Management Consultant Case Study",
            description = "Structure market sizing problems, formulate frameworks, and deliver executive summaries.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_8",
            title = "HR Generalist Cultural Interview",
            description = "Address workplace conflict mediation scenarios, policy compliance, and core culture fit.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_9",
            title = "Executive Leadership Evaluation",
            description = "Demonstrate strategic vision, empathy-focused delegation, and crisis scaling strategies.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_10",
            title = "Sales Representative Objection Pitch",
            description = "Handle cold-calling rejections, value prop delivery, and price objection negotiations.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_11",
            title = "DevOps Engineer Live Incident Case",
            description = "Diagnose high-priority server downtime, coordinate fixes, and present post-mortems.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_12",
            title = "Content Creator Brand Alignment Review",
            description = "Discuss audience metrics, digital sponsorship ethics, and creative growth strategies.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_13",
            title = "Customer Support Lead Interview",
            description = "Negotiate SLA priorities, handle upset account escalations, and manage team shifts.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_14",
            title = "Mobile App Architect Design Screen",
            description = "Discuss offline persistence, state patterns, memory leaks, and local SDK performance.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_15",
            title = "Business Analyst Requirement Gathering",
            description = "Bridge the gap between tech limits and user requests during mock stakeholder briefings.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_16",
            title = "AI Research Scientist Foundations Screen",
            description = "Discuss neural constraints, transformer parameters, bias metrics, and model fine-tuning.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "IM_17",
            title = "Project Manager Coordination Interview",
            description = "Resolve cross-functional blockages, define sprint goals, and manage critical paths.",
            mode = "HR Recruiter"
        )
    )

    val officeCorporateList = listOf(
        RolePlayTask(
            id = "OC_1",
            title = "Salary Negotiation with HR",
            description = "Negotiate base salary, signing bonuses, and equity packages professionally using benchmarks.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_2",
            title = "Conducting a Performance Review",
            description = "Deliver constructive feedback and remediation targets to a struggling direct report.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_3",
            title = "Handling an Angry Client Escalation",
            description = "Calm a frustrated enterprise client after a technical outage, committing to recovery.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_4",
            title = "Leading a Daily Agile Standup",
            description = "Provide a structured 60-second progress update detailing status and blocking bugs.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_5",
            title = "Pitching a High-Budget Proposal",
            description = "Secure capital funding from senior leadership by presenting ROI and risk mitigation.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_6",
            title = "Chiming In Politely during a Board debate",
            description = "Interrupt a senior executive politely to introduce a critical, overlooked risk factor.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_7",
            title = "Resolving a Technical Team Conflict",
            description = "Mediate a heated debate between two tech leads regarding database scaling patterns.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_8",
            title = "Explaining an Outage to Stakeholders",
            description = "Deliver immediate, transparent details regarding an ongoing high-priority security issue.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_9",
            title = "Cross-Cultural Team Onboarding",
            description = "Welcome global partners and establish common workspace communication standards.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_10",
            title = "Negotiating Project Scope with Managers",
            description = "Align resource limits and reject unfeasible feature requests without burning bridges.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_11",
            title = "Product Feature Brainstorming",
            description = "Engage in collaborative brainstorming, supporting ideas with positive framing devices.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_12",
            title = "Presenting Quarterly Sales Outcomes",
            description = "Describe visual trends and projections to the leadership team with maximum clarity.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_13",
            title = "Securing Critical Partner Buy-in",
            description = "Build immediate trust and rapport with a hesitant third-party vendor during negotiations.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_14",
            title = "Drafting and Explaining SLA Support Rules",
            description = "Establish clear response boundaries for customers without sounding dismissive.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_15",
            title = "Handling Confidential HR Disputes",
            description = "Engage with standard boundaries and utmost politeness during a sensitive workplace issue.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_16",
            title = "Mentoring a Struggling Junior Developer",
            description = "Coach an intern using active listening, descriptive advice, and confidence builders.",
            mode = "HR Recruiter"
        ),
        RolePlayTask(
            id = "OC_17",
            title = "Conducting a Post-Mortem Assessment",
            description = "Coordinate root-cause analysis and preventative measures after a major incident.",
            mode = "HR Recruiter"
        )
    )

    val dailyLifeList = listOf(
        RolePlayTask(
            id = "DL_1",
            title = "Ordering Food at a Premium Restaurant",
            description = "Practice polite dining phrases, dietary adjustments, and wine pairings with a sommelier.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_2",
            title = "Asking for Directions in a New City",
            description = "Inquire about transits and navigate landmarks using prepositions of movement.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_3",
            title = "Booking an Emergency Transit/Room",
            description = "Coordinate hotel bookings and travel itinerary shifts under severe delays.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_4",
            title = "Returning a Defective Item at a Store",
            description = "State consumer complaints politely and request immediate cash refunds from managers.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_5",
            title = "Icebreaker Conversation with a Stranger",
            description = "Initiate organic small talk, find mutual interests, and maintain friendly dynamics.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_6",
            title = "Expressing Sympathy to a Friend",
            description = "Deliver compassionate support and offer practical assistance to a grieving colleague.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_7",
            title = "Scheduling a Doctor Appointment",
            description = "Describe health symptoms and physical pain clearly to secure an urgent booking.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_8",
            title = "Discussing Book/Movie Recommendations",
            description = "Present captivating details of your favorite literature to convince an active reader.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_9",
            title = "Negotiating Airport Transit Delays",
            description = "Settle boarding disputes and claim complimentary vouchers from airport staff.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_10",
            title = "Discussing Personal Fitness Goals",
            description = "Explain workout habits, stamina metrics, and long-term wellness choices to a trainer.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_11",
            title = "Describing Warm Family Ties",
            description = "Describe close childhood bonds, ancestral heritage, and personal values.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_12",
            title = "Disputing a Bank Transaction Error",
            description = "Resolve double-charging billing issues with a bank customer representative.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_13",
            title = "Chasing Up a Late Package Delivery",
            description = "Inquire about transit logistics and demand expedited courier shipping.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_14",
            title = "Planning a Weekend Group Getaway",
            description = "Collaborate on travel schedules, accommodation bookings, and activity budgets.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_15",
            title = "Introducing Yourself to a New Neighbor",
            description = "Welcome local residents, share community tips, and establish immediate rapport.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_16",
            title = "Discussing Weather Commute Issues",
            description = "Discuss alternative transit routes during unexpected seasonal disruptions.",
            mode = "IELTS Examiner"
        ),
        RolePlayTask(
            id = "DL_17",
            title = "Asking a Critical Favor from a Friend",
            description = "Politely request urgent pet care or moving assistance using modal verbs.",
            mode = "IELTS Examiner"
        )
    )
}
