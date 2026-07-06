package com.example.data

data class ListeningExercise(
    val id: String,
    val title: String,
    val speaker: String,
    val audioText: String,
    val category: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

data class ReadingExercise(
    val id: String,
    val title: String,
    val passage: String,
    val category: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

data class WritingExercise(
    val id: String,
    val title: String,
    val prompt: String,
    val category: String,
    val minimumWords: Int = 100,
    val sampleAnswer: String
)

data class SpeakingExercise(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val initialQuestion: String,
    val description: String,
    val recommendedDuration: String
)

object PracticeData {

    val listeningList = listOf(
        ListeningExercise(
            id = "L1",
            title = "Introduction to Quantum Physics",
            speaker = "Dr. Arthur Pendelton",
            audioText = "Welcome to Physics 101. Today, we look at the fascinating realm of quantum mechanics. In classical physics, an object occupies a single state. However, in the quantum world, particles exist in a state called superposition. This means a particle can exist in multiple potential states simultaneously until it is measured. Once measured, the superposition collapses into a single reality.",
            category = "Academic Science",
            question = "What happens to a quantum particle when it is measured?",
            options = listOf(
                "It enters a state of high acceleration",
                "Its superposition collapses into a single state",
                "It splits into two identical particles",
                "It completely disappears from physical reality"
            ),
            correctAnswer = 1,
            explanation = "As Dr. Arthur explains, measuring a quantum particle causes its superposition to collapse into a single, definite state, unlike classical physics."
        ),
        ListeningExercise(
            id = "L2",
            title = "Mastering Job Interview Body Language",
            speaker = "Claire Vance (HR Consultant)",
            audioText = "When you walk into an interview, your verbal answers are only part of the equation. Your non-verbal communication speaks volumes. Maintaining consistent but natural eye contact displays confidence and sincerity. A firm handshake establishes trust instantly, while leaning slightly forward indicates active listening and interest in the position.",
            category = "Professional Development",
            question = "According to Claire, what does leaning slightly forward during an interview communicate?",
            options = listOf(
                "That you are feeling anxious or physically uncomfortable",
                "That you want the interview to end quickly",
                "That you are actively listening and interested in the role",
                "That you disagree with the interviewer's statements"
            ),
            correctAnswer = 2,
            explanation = "Claire explicitly states that leaning slightly forward during an interview indicates active listening and interest in the job."
        ),
        ListeningExercise(
            id = "L3",
            title = "Emergency Airport Announcement",
            speaker = "Terminal 2 Operator",
            audioText = "Attention all passengers scheduled for flight British Airways BA-217 to London Heathrow. Due to heavy fog over the English Channel, departure has been delayed by three hours. Passengers are advised to remain in the departure lounge. We will provide complimentary meal vouchers at Gate 14 starting in twenty minutes.",
            category = "Travel & Leisure",
            question = "What is the reason for the flight delay, and what is offered to passengers?",
            options = listOf(
                "Technical issues on the aircraft; complimentary hotel booking",
                "Heavy fog over the English Channel; complimentary meal vouchers",
                "Staff shortage in flight control; refund on ticket price",
                "Security check in progress; free airport lounge upgrades"
            ),
            correctAnswer = 1,
            explanation = "The announcement mentions the delay is due to heavy fog over the English Channel, and passengers can collect complimentary meal vouchers at Gate 14."
        ),
        ListeningExercise(
            id = "L4",
            title = "AI in Agriculture and Farming",
            speaker = "Dr. Elena Rostova",
            audioText = "Traditional farming is entering a smart era. By using IoT soil sensors and satellite imaging, modern AI models can determine exactly when crops need nitrogen, watering, or harvesting. This targeted agricultural system reduces water wastage by up to forty percent and significantly boosts overall crop yields, leading to highly sustainable farming practices.",
            category = "Tech & Agriculture",
            question = "By what percentage can smart agricultural systems reduce water wastage?",
            options = listOf(
                "Up to ten percent",
                "Up to forty percent",
                "Up to seventy-five percent",
                "Up to ninety-five percent"
            ),
            correctAnswer = 1,
            explanation = "Dr. Elena Rostova mentions that target smart systems reduce water wastage by up to forty percent."
        ),
        ListeningExercise(
            id = "L5",
            title = "Resolving a Customer Complaint",
            speaker = "Mark (Customer Success Lead)",
            audioText = "Hello, thank you for calling TechSphere. My name is Mark. I understand you received a damaged smart speaker yesterday. I sincerely apologize for the inconvenience. I have already authorized a free replacement package which will be shipped via express delivery today. I will also email you a prepaid shipping label to return the damaged item.",
            category = "Customer Service",
            question = "What action does Mark take to resolve the customer's problem?",
            options = listOf(
                "He offers a 50% discount on their next purchase",
                "He asks them to visit a local store to repair it",
                "He authorizes a free replacement and emails a prepaid return label",
                "He explains that shipping damage is not covered by warranty"
            ),
            correctAnswer = 2,
            explanation = "Mark resolves the issue by authorizing a free replacement shipped today and providing a prepaid return label for the damaged speaker."
        ),
        ListeningExercise(
            id = "L6",
            title = "The Roots of the Olympic Games",
            speaker = "Professor Julius Sterling",
            audioText = "The ancient Olympic Games began in Greece in 776 BC as a religious festival honoring Zeus. Held every four years, the games promoted peace through an official sacred truce, which paused ongoing wars among Greek city-states. This allowed athletes to travel and compete safely, laying the groundwork for international sportsmanship.",
            category = "History & Culture",
            question = "What primary purpose did the ancient sacred truce serve?",
            options = listOf(
                "It determined the supreme ruler of Greece",
                "It paused wars among Greek city-states so athletes could travel safely",
                "It collected taxes from spectators to fund temple construction",
                "It limited the number of sports allowed in the games"
            ),
            correctAnswer = 1,
            explanation = "The sacred truce paused ongoing wars to ensure athletes and spectators could travel safely to and from the Olympic Games."
        ),
        ListeningExercise(
            id = "L7",
            title = "Climate Change and Marine Life",
            speaker = "Dr. Sylvia Earle",
            audioText = "Marine ecosystems are reaching critical thresholds. As oceans absorb over ninety percent of excess heat from global warming, water temperatures rise. This leads to coral bleaching, where corals expel the symbiotic algae living inside them. Without this algae, corals lose their vibrant colors, turn white, and eventually starve to death.",
            category = "Ecology & Climate",
            question = "What directly causes corals to experience bleaching?",
            options = listOf(
                "Toxic chemical oil spills near major harbor ports",
                "Expelling symbiotic algae due to rising ocean water temperatures",
                "Overfishing of herbivorous fish that clean the reefs",
                "Lack of sunlight reaching deep ocean floors"
            ),
            correctAnswer = 1,
            explanation = "Corals bleach because rising temperatures stress them into expelling the symbiotic algae essential for their color and nutrition."
        ),
        ListeningExercise(
            id = "L8",
            title = "A Historic Walk Through London",
            speaker = "Arthur (Local Guide)",
            audioText = "To your left, you see the Tower of London, founded by William the Conqueror in 1066. While famous today for housing the Crown Jewels, it was originally built as a fortress to secure the city and project royal power. Over the centuries, it evolved into a notorious prison, holding high-profile figures accused of treason.",
            category = "Tourism & Travel",
            question = "What was the original purpose of building the Tower of London?",
            options = listOf(
                "To store the Crown Jewels safely from invaders",
                "To act as a public prison for common thieves",
                "To serve as a royal fortress to secure the city and project power",
                "To host theatrical plays and ancient tournaments"
            ),
            correctAnswer = 2,
            explanation = "The Tower of London was originally built as a defensive fortress to secure the city, and only later became a prison and treasury."
        ),
        ListeningExercise(
            id = "L9",
            title = "Cooking Secrets of Italian Risotto",
            speaker = "Chef Giovanni",
            audioText = "To make perfect risotto, you must never rinse the Arborio rice. Rinsing washes away the surface starch, which is what gives risotto its famous creamy texture. Pour your hot broth into the pan gradually, one ladle at a time, stirring continuously. This slow release of starch creates the rich, velvet sauce naturally.",
            category = "Culinary Arts",
            question = "Why does Chef Giovanni advise against rinsing Arborio rice before cooking?",
            options = listOf(
                "It makes the rice cook twice as slow",
                "Rinsing causes the rice to lose its natural shape and break",
                "It washes away surface starch needed to create a creamy texture",
                "It ruins the flavor by washing away added salt"
            ),
            correctAnswer = 2,
            explanation = "Not rinsing Arborio rice preserves the starch on the grain, which is essential to cook up risotto's classic creamy consistency."
        ),
        ListeningExercise(
            id = "L10",
            title = "Classical Conditioning in Psychology",
            speaker = "Dr. Susan Nolen",
            audioText = "In the late 1890s, Russian physiologist Ivan Pavlov discovered classical conditioning. He observed that dogs naturally salivated when given food. By pairing a bell chime with food delivery, the dogs learned to associate the bell with eating. Eventually, the bell alone was enough to trigger salivation, proving a conditioned response.",
            category = "Behavioral Science",
            question = "In Pavlov's experiment, what was the conditioned stimulus?",
            options = listOf(
                "The food given to the dogs",
                "The natural salivation response",
                "The chime of the bell",
                "The sound of the footsteps"
            ),
            correctAnswer = 2,
            explanation = "The bell chime is the conditioned stimulus, as it was a neutral sound that became associated with food, triggering the salivation response on its own."
        ),
        ListeningExercise(
            id = "L11",
            title = "The Logistics of Mars Colonization",
            speaker = "Dr. Charles Elachi",
            audioText = "Establishing a human settlement on Mars requires self-sufficiency. Because cargo flights take seven to nine months, resupply is extremely expensive. Future colonists must produce oxygen on Mars by extracting carbon dioxide from the thin Martian atmosphere and cracking it using high-temperature electrolysis.",
            category = "Space & Astronomy",
            question = "How will future Mars colonists produce breathable oxygen?",
            options = listOf(
                "By shipping large oxygen tanks from Earth continuously",
                "By extracting and cracking carbon dioxide from the Martian atmosphere",
                "By melting underground glaciers using geothermal energy",
                "By planting large pine tree forests in heated greenhouses"
            ),
            correctAnswer = 1,
            explanation = "Dr. Charles notes that colonists will extract and crack carbon dioxide from the local Martian atmosphere via electrolysis to produce oxygen."
        ),
        ListeningExercise(
            id = "L12",
            title = "Managing Chronic Stress Naturally",
            speaker = "Dr. Linda Hamilton",
            audioText = "Chronic stress releases continuous cortisol into the bloodstream, which can impair immune functions and raise blood pressure. To combat this, practicing diaphragmatic breathing for just ten minutes daily activates the parasympathetic nervous system, signaling the brain to reduce heart rates and induce deep calm.",
            category = "Health & Wellness",
            question = "How does diaphragmatic breathing help reduce chronic stress?",
            options = listOf(
                "It increases the production of adrenaline",
                "It activates the parasympathetic nervous system to slow heart rates",
                "It increases blood pressure to improve physical energy",
                "It burns off excess calories in the liver"
            ),
            correctAnswer = 1,
            explanation = "Deep diaphragmatic breathing activates the parasympathetic nervous system, inducing a relaxation response that slows heart rate and lowers stress."
        )
    )

    val readingList = listOf(
        ReadingExercise(
            id = "R1",
            title = "The Hidden Wonders of Hydrothermal Vents",
            passage = "Deep on the ocean floor, far beyond the reach of sunlight, lies a strange ecosystem powered by geothermal energy rather than the sun. Hydrothermal vents are underwater volcanic geysers that spew superheated, mineral-rich water into the freezing ocean. In the late 1970s, scientists discovered that these vents support complex communities of life, including giant tube worms, blind crabs, and vast mats of chemotrophic bacteria.\n\nUnlike terrestrial ecosystems that rely on photosynthesis, the base of the hydrothermal food web is chemosynthesis. Chemotrophic bacteria convert toxic hydrogen sulfide gas, escaping from the Earth's crust, into organic carbon molecules. This discovery revolutionized biology, showing that highly diverse life can thrive in pitch-black, high-pressure environments, raising hopes for finding extraterrestrial life in the oceans of icy moons like Europa.",
            category = "Deep-Sea Biology",
            question = "What biological process serves as the foundation of the food chain in hydrothermal vent ecosystems?",
            options = listOf(
                "Photosynthesis utilizing ambient bioluminescent light",
                "Chemosynthesis carried out by bacteria using hydrogen sulfide",
                "Decomposition of organic matter falling from the surface ocean",
                "Fermentation of minerals dissolved in cold deep seawater"
            ),
            correctAnswer = 1,
            explanation = "The text states that unlike terrestrial photosynthesis, the base of the hydrothermal food web is chemosynthesis, where bacteria convert hydrogen sulfide into organic carbon."
        ),
        ReadingExercise(
            id = "R2",
            title = "The Psychology of Flow State",
            passage = "Have you ever been so immersed in a task that you completely lost track of time and your surroundings? Psychologists refer to this optimal state of consciousness as 'flow.' Coined by Hungarian psychologist Mihaly Csikszentmihalyi, a flow state is characterized by intense, focused concentration, a merging of action and awareness, and a deep sense of enjoyment.\n\nAccording to Csikszentmihalyi, flow occurs when there is a perfect balance between the challenge of an activity and the skill level of the person performing it. If the task is too easy, boredom sets in. If the challenge exceeds the individual's skills, anxiety occurs. Only when the challenge matches the skill level, and clear, immediate goals are present, can an individual slide into flow, unlocking peak productivity and deep cognitive satisfaction.",
            category = "Cognitive Psychology",
            question = "Under what conditions does an individual enter a flow state?",
            options = listOf(
                "When a task is extremely simple and requires zero mental effort",
                "When there is an intense challenge that far exceeds their skill level",
                "When there is a perfect balance between the challenge and their skill level",
                "When they are working under strict supervision with tight deadlines"
            ),
            correctAnswer = 2,
            explanation = "According to Csikszentmihalyi, flow is achieved when there is a perfect balance between the challenge of the task and the skill level of the individual."
        ),
        ReadingExercise(
            id = "R3",
            title = "Renewable Energy: The Solar Breakthrough",
            passage = "Solar energy technology has advanced dramatically over the last decade. Early photovoltaic cells, primarily made of silicon, were heavy, expensive to manufacture, and operated at an efficiency rate of only ten to twelve percent. Today, researchers are shifting towards perovskite solar cells.\n\nPerovskite is a crystal material that can be manufactured at low temperatures using cheap chemical solutions. When combined with silicon in a tandem design, perovskite cells have reached experimental efficiency records exceeding thirty percent. This breakthrough promises to drastically lower the capital costs of solar farm installations, making green energy cheaper and more abundant than fossil fuels.",
            category = "Environmental Engineering",
            question = "What makes perovskite solar cells superior to traditional silicon-only cells?",
            options = listOf(
                "They are made from carbon-based fossil fuels",
                "They can be produced cheaply at low temperatures and offer higher efficiency in tandem designs",
                "They are completely immune to cloud coverage and rain",
                "They generate electricity even in total pitch-black conditions"
            ),
            correctAnswer = 1,
            explanation = "The passage highlights that perovskite crystal can be manufactured at low temperatures with cheap chemicals and achieves over 30% efficiency in tandem designs."
        ),
        ReadingExercise(
            id = "R4",
            title = "The Silk Road: Ancient Globalization",
            passage = "The Silk Road was not a single, continuous road, but rather a vast network of ancient trade routes spanning over six thousand kilometers, connecting China, Central Asia, India, Persia, and the Mediterranean. Established during China's Han Dynasty around 130 BC, this transcontinental trade network carried more than just physical goods like silk, tea, porcelain, and spices.\n\nCrucially, the Silk Road served as a vital channel for cultural exchange. Along with merchants, travelers, and pilgrims, ideas, scientific discoveries, and religious beliefs moved across borders. Buddhism traveled from India to East Asia, paper-making technology moved from China to the Islamic world, and artistic motifs fused Greek, Persian, and Indian aesthetics. It was, in essence, the world's first major engine of globalization.",
            category = "World History",
            question = "What was the broader, non-commercial significance of the Silk Road?",
            options = listOf(
                "It established a unified global government under the Han Dynasty",
                "It operated as a channel for cultural exchange, moving religions, ideas, and technologies across borders",
                "It eliminated the need for naval ships or maritime trade",
                "It created a single global currency accepted by all empires"
            ),
            correctAnswer = 1,
            explanation = "The passage notes that the Silk Road's crucial impact was acting as a vital channel for cultural exchange, spreading ideas, religion, and science."
        ),
        ReadingExercise(
            id = "R5",
            title = "Cognitive Biases in Daily Decision Making",
            passage = "Human beings like to think of themselves as rational decision-makers, but our brains rely on mental shortcuts called heuristics to process information quickly. While heuristics are useful for survival, they often lead to systematic errors in thinking known as cognitive biases.\n\nOne of the most prevalent is 'confirmation bias.' This occurs when we actively search for, interpret, and remember information in a way that confirms our pre-existing beliefs, while ignoring or dismissing evidence that contradicts them. Confirmation bias can create echo chambers on social media, lead to poor financial investments, and prevent objective evaluation of complex issues. Overcoming this bias requires conscious effort to seek out alternative viewpoints and question our assumptions.",
            category = "Behavioral Science",
            question = "What is the defining characteristic of 'confirmation bias'?",
            options = listOf(
                "A tendency to forget information after making a major decision",
                "Preferring information that aligns with our existing beliefs while ignoring contrary evidence",
                "Feeling highly anxious when forced to make a decision quickly",
                "Relying on physical senses rather than mathematical logic"
            ),
            correctAnswer = 1,
            explanation = "The passage defines confirmation bias as searching for, interpreting, and recalling information in a way that confirms existing beliefs while dismissing contradictory evidence."
        ),
        ReadingExercise(
            id = "R6",
            title = "The Engineering of Gothic Architecture",
            passage = "In medieval Europe, the Romanesque architectural style, characterized by thick walls and heavy rounded arches, severely limited the height and light of cathedrals. This changed in the 12th century with the birth of Gothic architecture. Master builders developed three structural innovations: pointed arches, ribbed vaults, and flying buttresses.\n\nPointed arches distributed weight downward more efficiently than round arches. Ribbed vaults supported taller stone ceilings with less bulk. Most importantly, flying buttresses-external arched stone supports-anchored the lateral thrust of the high walls, transferring the weight directly to heavy external piers. This freed the walls from carrying the main load, allowing architects to replace massive stone walls with tall, elegant stained-glass windows, filling the interior with celestial light.",
            category = "History of Architecture",
            question = "How did flying buttresses transform the interior design of Gothic cathedrals?",
            options = listOf(
                "They allowed architects to build secret underground dungeons",
                "They removed load-bearing stress from walls, enabling high stained-glass windows",
                "They replaced pointed arches with heavy Romanesque arches",
                "They allowed buildings to be constructed out of wood instead of stone"
            ),
            correctAnswer = 1,
            explanation = "The text explains that flying buttresses anchored the lateral thrust, freeing walls from bearing the weight and allowing architects to insert large stained-glass windows."
        ),
        ReadingExercise(
            id = "R7",
            title = "Microplastics: The Invisible Aquatic Threat",
            passage = "Microplastics are tiny plastic particles measuring less than five millimeters in size. They originate from two sources: primary microplastics, which are manufactured directly for cosmetics or industrial use, and secondary microplastics, which result from the physical fragmentation of larger plastic waste like water bottles and synthetic fibers.\n\nBecause they do not biodegradable, microplastics accumulate in marine environments. Marine organisms, from tiny zooplankton to large fish, mistake these particles for food. This introduces toxic chemicals, such as heavy metals and endocrine disruptors, into the aquatic food web. As larger fish consume smaller ones, these toxins bioaccumulate, eventually posing health risks to human consumers at the top of the food chain.",
            category = "Marine Ecology",
            question = "What is the difference between primary and secondary microplastics?",
            options = listOf(
                "Primary plastics are biodegradable, whereas secondary plastics are toxic",
                "Primary microplastics are directly manufactured, while secondary microplastics break off from larger plastic items",
                "Primary microplastics float on the ocean surface, while secondary plastics sink to the floor",
                "Primary microplastics are made in labs, while secondary microplastics occur naturally"
            ),
            correctAnswer = 1,
            explanation = "According to the passage, primary microplastics are directly manufactured as tiny particles, whereas secondary microplastics arise from the breakdown of larger plastic items."
        ),
        ReadingExercise(
            id = "R8",
            title = "Smart Cities of the Future",
            passage = "Rapid urbanization is putting unprecedented strain on municipal infrastructure. To manage this growth, cities are turning to the Internet of Things (IoT) to build 'smart cities.' By deploying networks of connected sensors, cameras, and automated systems, cities can manage resources in real-time.\n\nIn Barcelona, for example, smart streetlights automatically adjust their brightness based on pedestrian movement, saving energy. Smart waste bins alert collection trucks when they are full, optimizing fuel-efficient routes. Connected water grids detect microscopic leaks instantly, preventing millions of gallons of water loss. Through these integrations, smart cities reduce operational costs, lower carbon footprints, and dramatically improve the quality of life for residents.",
            category = "Urban Planning",
            question = "How do smart waste bins contribute to municipal efficiency in smart cities?",
            options = listOf(
                "By sorting and recycling glass and plastic automatically",
                "By incinerating waste internally to produce heat",
                "By notifying collection trucks when they are full to optimize driving routes",
                "By charging residents a fee based on the weight of their garbage"
            ),
            correctAnswer = 2,
            explanation = "The text states that smart waste bins optimize efficiency by alerting collection trucks when they are full, saving fuel and time."
        ),
        ReadingExercise(
            id = "R9",
            title = "The Decipherment of the Rosetta Stone",
            passage = "For centuries, the hieroglyphic writing of ancient Egypt was a lost language, its meaning completely forgotten. This changed in 1799, when French soldiers discovering a black granodiorite slab near the town of Rosetta unearthed what would become known as the Rosetta Stone.\n\nThe stone featured a single decree issued by King Ptolemy V in 196 BC, written in three distinct scripts: Ancient Egyptian hieroglyphs, Demotic script, and Ancient Greek. Because Greek was well known, scholars had a direct translation key. In 1822, French linguist Jean-François Champollion made the crucial breakthrough, realizing that hieroglyphs functioned both as phonetic symbols representing sounds and as ideograms representing ideas, unlocking three thousand years of Egyptian history.",
            category = "Archaeology & Linguistics",
            question = "Why was the Rosetta Stone so crucial in deciphering Egyptian hieroglyphs?",
            options = listOf(
                "It contained a detailed map showing the locations of hidden tombs",
                "It presented the same royal decree in three scripts, including easily readable Ancient Greek",
                "It was written by King Ptolemy V himself using a special code",
                "It was the oldest piece of writing ever discovered in human history"
            ),
            correctAnswer = 1,
            explanation = "The stone was crucial because it recorded a single decree in three scripts (including Ancient Greek), which scholars could read and use as a direct key."
        ),
        ReadingExercise(
            id = "R10",
            title = "The Vital Importance of Sleep to Brain Health",
            passage = "For decades, sleep was viewed as a passive state of rest for the body. However, modern neuroscience reveals that the brain is highly active during sleep, performing essential maintenance tasks. One of the most important functions is the operation of the 'glymphatic system.'\n\nDuring deep sleep, the spaces between brain cells expand, allowing cerebrospinal fluid to wash through the brain and flush out metabolic waste products. This includes beta-amyloid, a protein that forms toxic plaques associated with Alzheimer's disease. Furthermore, during REM sleep, the brain consolidates memories, transferring temporary experiences into long-term storage and pruning weak neural connections, making sleep non-negotiable for mental health.",
            category = "Neuroscience",
            question = "What is the primary role of the glymphatic system during deep sleep?",
            options = listOf(
                "To generate vivid dreams that improve creative thinking",
                "To flush metabolic waste products, like toxic beta-amyloid proteins, from the brain",
                "To reduce the amount of oxygen the brain consumes",
                "To repair damaged skull bones and muscle tissues"
            ),
            correctAnswer = 1,
            explanation = "The passage states that the glymphatic system washes cerebrospinal fluid through the brain during deep sleep to flush out waste products like beta-amyloid."
        ),
        ReadingExercise(
            id = "R11",
            title = "The Economic Mechanism of Inflation",
            passage = "In economics, inflation is the general increase in prices and fall in the purchasing value of money over time. It is driven primarily by two mechanisms: demand-pull inflation and cost-push inflation. Demand-pull occurs when aggregate demand for goods outpaces aggregate supply, often described as 'too much money chasing too few goods.'\n\nOn the other hand, cost-push inflation is driven by an increase in the prices of raw materials or wages, forcing companies to raise their retail prices to maintain profit margins. Central banks manage inflation by adjusting interest rates. Raising interest rates makes borrowing more expensive, which cools economic activity, curbs consumer spending, and stabilizes prices.",
            category = "Macroeconomics",
            question = "What is 'demand-pull' inflation?",
            options = listOf(
                "When companies lower prices to attract more buyers",
                "When aggregate demand for goods outpaces supply, raising prices",
                "When central banks print money to buy gold reserves",
                "When rising oil prices increase transportation costs"
            ),
            correctAnswer = 1,
            explanation = "The text defines demand-pull inflation as occurring when aggregate demand for goods outpaces aggregate supply ('too much money chasing too few goods')."
        ),
        ReadingExercise(
            id = "R12",
            title = "The Florence Renaissance and Scientific Method",
            passage = "The Renaissance, which began in the city-state of Florence during the 14th century, is celebrated as a golden age of art and literature. However, its most profound contribution was arguably the birth of the scientific method. For centuries, scholasticism dominated European thought, relying on ancient scriptures and religious authority to explain the natural world.\n\nRenaissance humanists shifted the focus toward empirical observation and experimentation. Pioneers like Leonardo da Vinci and later Galileo Galilei argued that theories must be tested against physical observation and mathematical proof. This intellectual shift challenged dogma, eventually triggering the Scientific Revolution and laying the foundations for modern physics, engineering, and anatomy.",
            category = "History of Science",
            question = "How did Renaissance humanists challenge the medieval scholastic approach to explaining nature?",
            options = listOf(
                "By translating ancient scrolls into modern languages",
                "By rejecting mathematics entirely in favor of artistic paintings",
                "By prioritizing empirical observation and physical experimentation over dogma",
                "By building larger libraries and monasteries across Europe"
            ),
            correctAnswer = 2,
            explanation = "The Renaissance humanists shifted scientific inquiry towards empirical observation and experimentation, moving away from relying on religious scriptures."
        )
    )

    val writingList = listOf(
        WritingExercise(
            id = "W1",
            title = "IELTS Academic Task 1: Global Internet Trends",
            prompt = "The line graph below shows the percentage of the population using the internet in three countries (USA, Mexico, and Japan) from 1999 to 2019. Summarize the information by selecting and reporting the main features, and make comparisons where relevant.\n\n(Data: USA rose from 20% to 90%, Japan rose from 10% to 85%, Mexico rose from 5% to 65% in the twenty-year span).",
            category = "Academic Chart Description",
            minimumWords = 150,
            sampleAnswer = "The line graph illustrates the percentage of internet users in the USA, Japan, and Mexico over a twenty-year period from 1999 to 2019. Overall, all three nations experienced significant growth in internet adoption, with the USA maintaining the highest percentage of users throughout the entire timeline.\n\nIn 1999, the USA led with 20% internet penetration, followed by Japan at 10% and Mexico at a mere 5%. Over the next decade, internet usage in the USA and Japan surged rapidly. By 2009, approximately 70% of the US population was online, while Japan overtook Mexico significantly to reach 55% penetration. Mexico experienced slower initial growth, reaching only 25% during the same year.\n\nBy 2019, internet access had become nearly universal in both the USA and Japan, peaking at 90% and 85% respectively. Mexico, while still lagging behind the other two nations, witnessed a sharp acceleration in adoption during the final decade, culminating at a substantial 65% in 2019."
        ),
        WritingExercise(
            id = "W2",
            title = "IELTS Task 2: AI and Future Unemployment",
            prompt = "Some people believe that artificial intelligence and automation will cause mass unemployment and ruin careers, while others argue it will create highly skilled new jobs. Discuss both views and give your opinion.",
            category = "Discussion & Opinion Essay",
            minimumWords = 250,
            sampleAnswer = "The rapid advancement of artificial intelligence and machine learning has sparked a fierce debate regarding its impact on the workforce. While critics argue that automation will decimate industries and cause widespread job losses, optimists contend that AI will usher in a new era of high-value career opportunities. This essay will examine both perspectives before presenting my view.\n\nOn the one hand, there are legitimate fears that automation will replace blue-collar and administrative roles. AI algorithms can analyze massive quantities of data faster and more accurately than humans. Consequently, jobs in customer service, data entry, bookkeeping, and even basic accounting are highly vulnerable. In manufacturing and logistics, autonomous vehicles and robotic arms are poised to displace millions of drivers and warehouse staff. For these workers, the threat of redundancy is real, and transitioning into new fields can be difficult and costly.\n\nOn the other hand, history has shown that every technological revolution creates more jobs than it destroys. The industrial and computer revolutions displaced traditional workers but gave rise to massive tech, service, and manufacturing sectors. Similarly, AI requires human oversight. New roles such as machine learning engineers, data scientists, AI ethicists, and prompt engineers are surging in demand. Furthermore, by automating mundane, repetitive tasks, AI frees humans to focus on tasks requiring emotional intelligence, critical thinking, and creativity—qualities machines cannot replicate.\n\nIn my opinion, while AI will undoubtedly disrupt the job market in the short term, it will not lead to permanent mass unemployment. Instead, it will redefine the nature of work. To mitigate the negative impacts, governments and corporations must proactively invest in nationwide retraining and upskilling programs. Ultimately, AI should be viewed not as a replacement for human intellect, but as a powerful tool to augment our capabilities."
        ),
        WritingExercise(
            id = "W3",
            title = "Formal Email: Project Delay Notification",
            prompt = "You are a Project Lead in a software firm. Write a formal email to an external client, Mr. Henderson, explaining that your software release will be delayed by two weeks due to a crucial security patch. Apologize sincerely, outline the issue, state the new date, and invite questions.",
            category = "Business Communication",
            minimumWords = 100,
            sampleAnswer = "Subject: Status Update: Deliverable Timeline Adjustment - Alpha CRM Platform\n\nDear Mr. Henderson,\n\nI hope this email finds you well.\n\nI am writing to provide an important update regarding the upcoming release of your customized CRM software platform. During our final staging tests yesterday, our quality assurance team identified an edge-case vulnerability in our data encryption pipeline. \n\nTo guarantee the absolute security of your customer records, we have made the decision to implement a robust security patch before deployment. Consequently, our release date will be adjusted by two weeks, with the new final launch scheduled for Friday, July 24th.\n\nWe sincerely apologize for this delay and any inconvenience it may cause to your onboarding schedule. However, we believe that launching a highly secure, flawless product is paramount. \n\nPlease let me know if you would like to arrange a brief call next Monday to review our patch details.\n\nSincerely,\n\nMark Fletcher\nLead Systems Architect, TechSphere"
        ),
        WritingExercise(
            id = "W4",
            title = "IELTS Task 2: Impact of Mass Tourism",
            prompt = "International tourism has become a major industry in many developing nations. While it brings significant economic benefits, some argue it damages local environments and dilutes traditional cultures. Discuss the pros and cons of mass tourism.",
            category = "Balanced Discussion Essay",
            minimumWords = 250,
            sampleAnswer = "In recent decades, global travel has become cheaper and more accessible, turning tourism into a key economic driver for many developing countries. However, this influx of visitors has also raised concerns about ecological damage and cultural erosion. This essay will weigh the advantages and disadvantages of international tourism.\n\nOn the positive side, tourism is a powerful engine for economic development and job creation. It brings vital foreign currency into the country, boosting hospitality, transport, and local retail sectors. For millions of residents in developing areas, tourist spending is their primary source of income. Furthermore, governments often utilize tourism revenues to develop infrastructure, building modern airports, highways, and public transit systems that benefit local citizens. It also encourages the preservation of historic landmarks and national parks, which serve as key tourist attractions.\n\nOn the negative side, unregulated mass tourism can devastate local ecosystems. Popular beach destinations often suffer from water pollution, coral reef destruction, and plastic waste accumulation. Overdevelopment of hotels can lead to deforestation and loss of wildlife habitats. Additionally, the constant exposure to foreign commercialism can dilute traditional cultures. Young locals may discard their ancestral customs, dialects, and values in favor of Western lifestyles, leading to a homogenization of global cultures. Inflation is another issue, as rising prices of food and housing near tourist spots can make daily life unaffordable for local residents.\n\nIn conclusion, while international tourism offers immense economic opportunities, it must not be allowed to thrive at the expense of the environment and cultural identity. Governments must implement strict sustainable tourism policies, limit visitor numbers in fragile ecosystems, and actively support local cultural heritage. Balanced, well-managed travel is key to ensuring both tourists and host communities thrive."
        ),
        WritingExercise(
            id = "W5",
            title = "Technical Writing: Intro to REST APIs",
            prompt = "Write an introductory explanation of how a REST API works for a non-technical beginner. Use the classic restaurant analogy (customer, waiter, kitchen) to explain client, request, API, and server.",
            category = "Technical Documentation",
            minimumWords = 120,
            sampleAnswer = "If you have ever wondered how different apps on your phone share information, the answer is a REST API. Think of a REST API as a friendly waiter in a restaurant.\n\nYou, the user sitting at the table, represent the 'client' (like your web browser). The 'kitchen' in the back of the restaurant represents the 'server' where all the delicious food and data is stored. You cannot walk directly into the kitchen to grab food; instead, you look at a menu and make an order.\n\nThe waiter is the 'API.' The waiter takes your order (called a 'Request') to the kitchen, tells the chef what you want, and then carries the delicious meal back to your table (called the 'Response'). \n\nSimilarly, when you check the weather on your phone, the app sends a request through the REST API waiter to retrieve current temperatures from a remote data kitchen, delivering it securely to your screen in seconds."
        ),
        WritingExercise(
            id = "W6",
            title = "IELTS General Task 1: Letter to Landlord",
            prompt = "You are renting an apartment, and the kitchen pipe has been leaking for three days. Your landlord, Mr. Davies, has not responded to your phone calls. Write a formal letter explaining the issue, describing the damage, and requesting urgent repairs.",
            category = "Formal Complaint Letter",
            minimumWords = 150,
            sampleAnswer = "Dear Mr. Davies,\n\nI am writing to formally report an urgent maintenance issue in Apartment 4B. The main water supply pipe underneath the kitchen sink has developed a severe leak, which has been flowing continuously for the past three days.\n\nDespite my multiple attempts to contact you by telephone since Tuesday, I have not received a reply. The situation has now escalated, as the pooling water has warped the wooden cabinets beneath the sink and is starting to leak through the floorboards into the hallway carpet, creating a damp odor.\n\nTo prevent further structural damage to your property, this leak requires immediate professional repair. I request that you dispatch an emergency plumber to the apartment within the next twenty-four hours.\n\nI would appreciate your immediate confirmation of when the plumber will arrive. I can be reached directly at 555-0192.\n\nSincerely,\n\nEleanor Rigby"
        ),
        WritingExercise(
            id = "W7",
            title = "Opinion Essay: Space Exploration Funding",
            prompt = "Governments spend billions of dollars on space exploration and Mars missions. Critics argue this money should be spent solving critical problems on Earth, such as poverty and climate change. To what extent do you agree or disagree?",
            category = "Persuasive Opinion Essay",
            minimumWords = 250,
            sampleAnswer = "The debate surrounding space exploration funding is highly contentious. Many argue that allocating billions of dollars to probe distant planets is a wasteful luxury when our planet faces urgent crises like poverty, famine, and environmental degradation. While I acknowledge the weight of these concerns, I disagree with the view that we should stop funding space programs, as space exploration yields immense, direct benefits for life on Earth.\n\nFirst and foremost, space exploration is a powerful catalyst for technological and scientific breakthroughs. Many technologies we rely on daily were originally developed for space missions. For instance, advanced water purification filters, solar panels, scratch-resistant lenses, and modern GPS satellite mapping were all pioneered by space agencies. GPS technology alone has revolutionized agriculture, shipping, global logistics, and weather forecasting, saving countless lives through early storm detection.\n\nFurthermore, studying other planets provides critical insights into our own environment. Space satellites monitor Earth's ice sheets, deforestation rates, and atmospheric carbon levels in real-time, providing scientists with the essential data needed to analyze and combat climate change. Venus, with its runaway greenhouse effect, serves as an invaluable warning model of what could happen to Earth if environmental damage is left unchecked. Ceasing space research would leave us blind to major global environmental trends.\n\nIn conclusion, space exploration is not an alternative to solving Earth's problems; rather, it is a key tool in our efforts to solve them. While we must continue to fund social welfare and environmental protection, we should view space budgets not as an expensive hobby, but as an active, long-term investment in our collective scientific future."
        ),
        WritingExercise(
            id = "W8",
            title = "Proposal: Green Office Transformation",
            prompt = "Your company's office currently wastes a lot of paper and electricity. Write a structured proposal to your general manager suggesting three specific, low-cost green initiatives to reduce waste and improve energy conservation.",
            category = "Business Proposal",
            minimumWords = 150,
            sampleAnswer = "To: General Manager\nFrom: Operations Team\nDate: July 2, 2026\nSubject: Proposal: Low-Cost Green Office Transformation\n\nIntroduction\nOur office currently experiences high paper consumption and energy waste. This proposal outlines three low-cost, highly effective initiatives to reduce our environmental impact and lower utility expenses.\n\n1. Transition to 'Default Duplex' Printing\nCurrently, we print single-sided documents by default, wasting reams of paper. By reconfiguring all office printers to double-sided (duplex) printing by default, we can immediately reduce paper waste by an estimated forty percent.\n\n2. Implementation of Smart Power Strips\nElectronics consume standby energy even when turned off. Installing smart power strips in all cubicles will automatically cut off power to computer monitors, desk lamps, and printers when employees sign out, lowering electricity bills.\n\n3. Centralized Recycling and Composites\nReplacing individual under-desk trash cans with a single, centralized recycling station in the breakroom will encourage mindful sorting, reducing our landfill contribution and improving overall workplace sustainability.\n\nConclusion\nThese three initiatives require minimal capital investment but will deliver immediate cost-savings while demonstrating our commitment to corporate environmental responsibility."
        ),
        WritingExercise(
            id = "W9",
            title = "IELTS Task 2: Sugar and Health Taxes",
            prompt = "Obesity and related chronic illnesses are rising globally. Some experts suggest that placing high taxes on sugary foods and carbonated drinks is the best way to curb consumption. Discuss this proposal and give your opinion.",
            category = "Argumentative Opinion Essay",
            minimumWords = 250,
            sampleAnswer = "In recent years, the global surge in obesity, diabetes, and cardiovascular diseases has put immense strain on healthcare systems. To tackle this, several public health advocates have proposed introducing heavy taxes on sugary foods and carbonated beverages. While opponents argue this is unfair to consumers and business owners, I believe that implementing sugar taxes is a highly effective, necessary strategy to improve public health.\n\nOn the one hand, critics argue that sugar taxes are regressive, disproportionately affecting low-income families who rely on cheap, high-calorie foods. They contend that a tax on soft drinks will not change eating habits but will simply increase the cost of living. Furthermore, some believe that food choices are a matter of personal freedom, and governments should not act as a parental authority dictating what citizens eat or drink.\n\nOn the other hand, empirical evidence from countries like Mexico and the UK shows that sugar taxes work. When a sugar levy was introduced in the UK, many major beverage manufacturers chose to reformulate their recipes, reducing the sugar content of their drinks to avoid the tax. This led to a massive reduction in sugar consumption without raising prices for consumers. Additionally, the revenue generated from sugar taxes can be directly reinvested into funding public healthcare, school lunches, and sports programs, creating a double benefit for society.\n\nIn my opinion, sugary drinks are not a necessity, but a luxury with severe public health consequences. Just as high taxes on tobacco successfully reduced smoking rates and saved lives, a sugar tax will signal the dangers of excessive sugar consumption and encourage healthier dietary habits. Combined with public education campaigns, sugar taxes are a vital step toward a healthier society."
        ),
        WritingExercise(
            id = "W10",
            title = "Speculative Essay: Daily Life in 2075",
            prompt = "How will technology alter daily human life fifty years from now? Write a speculative essay describing home, work, and transportation in the year 2075.",
            category = "Speculative Writing",
            minimumWords = 150,
            sampleAnswer = "By the year 2075, the boundary between physical and digital worlds will have completely dissolved. Life will be marked by absolute integration with advanced technologies, dramatically reshaping our homes, careers, and transit systems.\n\nAt home, smart materials will adapt instantly to our biological needs. AI-driven systems will manage household chores, optimize energy consumption, and synthesize personalized, nutritious meals using recycled organic components. Traditional furniture will be replaced by fluid, holographic spaces that can morph from a cozy living room into an immersive virtual workspace.\n\nWork itself will shift entirely from routine management to high-level creativity and strategic oversight, as robots and synthetic agents handle all physical and digital operations. Humans will work collaboratively across the globe in virtual sandboxes, utilizing neural interfaces to exchange thoughts at the speed of light.\n\nTransportation will be completely automated and zero-emission. Cities will feature multi-layered magnetic levitation tracks, routing silent passenger capsules smoothly across urban centers, eliminating traffic congestion and pollution forever."
        ),
        WritingExercise(
            id = "W11",
            title = "IELTS Academic Task 1: Paper Recycling Process",
            prompt = "Describe the industrial process of recycling paper from waste to usable pulp and finished paper. Focus on the main steps: collection, sorting, pulping, cleaning, de-inking, and pressing.",
            category = "Industrial Process Description",
            minimumWords = 150,
            sampleAnswer = "The process diagram outlines the multi-stage industrial method used to recycle waste paper into high-quality, reusable paper sheets. Overall, the process consists of six primary stages, transforming raw waste paper into pristine rolls through intensive physical and chemical treatments.\n\nFirst, waste paper is collected from recycling bins and transported to a processing plant, where it is sorted by quality and type. In the second stage, the sorted paper is combined with water and specialized chemicals in a giant vat called a pulper, which breaks down the paper fibers into a thick slurry called pulp.\n\nThird, the pulp passes through centrifuge filters, which screen out contaminants like staples, plastic clips, and glue. Following this, the pulp undergoes de-inking, where air bubbles and soap are introduced to float ink particles away from the clean wood fibers. Finally, the clean pulp is mixed with fresh water, spread onto moving screens, and pressed through heated rollers to squeeze out moisture, drying it into long sheets of finished recycled paper."
        ),
        WritingExercise(
            id = "W12",
            title = "IELTS Task 2: Online vs. Offline Education",
            prompt = "Some educators argue that virtual classrooms and online courses are more effective than traditional university lectures. Others argue that physical interaction on campus is essential for learning and personal growth. Discuss both views and give your opinion.",
            category = "Educational Philosophy",
            minimumWords = 250,
            sampleAnswer = "The rise of digital technology has transformed global education, making remote learning a major alternative to traditional on-campus university programs. While online platforms offer unprecedented flexibility, physical campuses provide vital social dynamics. This essay will discuss both methods of education before explaining my support for campus-based learning.\n\nOn the one hand, online education offers unmatched convenience. Students can access world-class lectures from anywhere in the world, eliminating geographical barriers. Remote learning is highly cost-effective, saving students thousands of dollars on commuting, campus housing, and expensive textbooks. Furthermore, asynchronous online courses allow adult learners to study at their own pace while maintaining full-time careers or family responsibilities, democratizing access to higher education.\n\nOn the other hand, the university experience is about more than just reading slides and passing tests. Physical classrooms foster active discussion, spontaneous debate, and collaborative project work that develops critical communication and leadership skills. Being on campus exposes young adults to diverse cultures, networking opportunities, and lifelong friendships. This social development is essential for building emotional intelligence and maturity. Many specialized fields, such as chemistry, medicine, and engineering, also require physical lab equipment and hands-on guidance that simulation software cannot fully replicate.\n\nIn my opinion, while online courses are excellent for professional certifications and vocational training, they cannot replace the rich, holistic environment of a physical university. A hybrid model, combining the flexibility of digital lectures with mandatory in-person seminars and workshops, represents the absolute future of higher education."
        )
    )

    val speakingList = listOf(
        SpeakingExercise(
            id = "S1",
            title = "Art & Creative AI",
            category = "Tech & AI Society",
            prompt = "Discuss the impact of generative AI on painters, writers, and traditional craftsmanship.",
            initialQuestion = "How do you feel artificial intelligence is impacting the creative industries such as art and writing?",
            description = "Discuss human creativity vs machine automation with Devansh Sir.",
            recommendedDuration = "4-5 minutes"
        ),
        SpeakingExercise(
            id = "S2",
            title = "Architecture Trade-offs",
            category = "Software Engineering",
            prompt = "Describe a tough technical decision where you had to sacrifice something for system performance.",
            initialQuestion = "How do you approach designing a large-scale software system to be both scalable and maintainable?",
            description = "STAR method interview on engineering architecture.",
            recommendedDuration = "3-4 minutes"
        ),
        SpeakingExercise(
            id = "S3",
            title = "Product Prioritization",
            category = "Product Management",
            prompt = "Explain your framework for prioritizing feature requests from aggressive stakeholders.",
            initialQuestion = "What strategy do you employ to prioritize a product backlog when faced with competing stakeholders?",
            description = "RICE scoring and product roadmap discussion.",
            recommendedDuration = "4 minutes"
        ),
        SpeakingExercise(
            id = "S4",
            title = "Memorable Travels",
            category = "General English",
            prompt = "Speak about a beautiful geographical location or cultural town you explored recently.",
            initialQuestion = "Could you describe a place you visited recently that made a lasting impression on you?",
            description = "Describe sights, sounds, and local cultures.",
            recommendedDuration = "2-3 minutes"
        ),
        SpeakingExercise(
            id = "S5",
            title = "Climate Change & Oceans",
            category = "Ecology & Science",
            prompt = "Discuss rising temperatures, plastic contamination, and sustainable marine protection.",
            initialQuestion = "How can individual citizens help protect marine environments and reduce plastic pollution?",
            description = "Environmental awareness and active recycling ideas.",
            recommendedDuration = "3-4 minutes"
        ),
        SpeakingExercise(
            id = "S6",
            title = "Gaining Fluent Confidence",
            category = "IELTS Speaking",
            prompt = "Describe your personal schedule and habits for learning and practicing English.",
            initialQuestion = "Why is speaking English confidently so important for your career goals?",
            description = "Daily habits, vocabulary improvement, and presentation skills.",
            recommendedDuration = "3 minutes"
        )
    )
}
