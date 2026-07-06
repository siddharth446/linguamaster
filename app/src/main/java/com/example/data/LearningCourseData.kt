package com.example.data

data class InteractiveQuiz(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

data class LearningChapter(
    val id: String,
    val title: String,
    val subtitle: String,
    val coreConcept: String,
    val vocabulary: List<Pair<String, String>>,
    val quiz: InteractiveQuiz
)

object LearningCourseData {

    val generalEnglishList = listOf(
        LearningChapter(
            id = "GE_C1",
            title = "Chapter 1: Advanced Sentence Structure",
            subtitle = "Complex, Compound, Compound-Complex Sentences & Parallelism",
            coreConcept = "Advanced sentence construction leverages a combination of coordination and subordination. Complex sentences use subordinating conjunctions (e.g., 'although', 'while') to join independent and dependent clauses. Compound-complex sentences contain at least two independent clauses and one dependent clause. Mastering parallelism ensures that elements in a list or comparison have the same grammatical structure.",
            vocabulary = listOf(
                "Subordination" to "The process of linking a dependent clause to an independent clause.",
                "Parallelism" to "The matching of sentence structures, verbs, or nouns in a series.",
                "Inversion" to "Reversing the standard subject-verb order for emphasis (e.g., 'Seldom have I seen...')."
            ),
            quiz = InteractiveQuiz(
                question = "Which of the following is an example of correct parallelism?",
                options = listOf(
                    "He likes swimming, to jog, and playing tennis.",
                    "He likes swimming, jogging, and playing tennis.",
                    "He likes to swim, jogging, and play tennis.",
                    "He likes swimming, jogging, and to play tennis."
                ),
                correctAnswer = 1,
                explanation = "Matching '-ing' verbs (swimming, jogging, playing) ensures perfect structural parallelism."
            )
        ),
        LearningChapter(
            id = "GE_C2",
            title = "Chapter 2: Mastering All Tenses",
            subtitle = "Advanced Usage & Narrative Tenses",
            coreConcept = "Narrative tenses (past simple, past continuous, past perfect, and past perfect continuous) establish background actions and chronological sequences. Advanced tense structures include future-in-the-past (e.g., 'was going to achieve'), which describes actions that were planned but may or may not have happened.",
            vocabulary = listOf(
                "Narrative tenses" to "Verbs used to set the scene, outline events, and give background in stories.",
                "Future in the past" to "A verb tense used to express a future plan from the perspective of the past.",
                "Stative verbs" to "Verbs that describe states rather than physical actions, which rarely use continuous forms."
            ),
            quiz = InteractiveQuiz(
                question = "Choose the correct tense: 'By the time the manager arrived, we ______ solving the issue.'",
                options = listOf(
                    "already finished",
                    "had already finished",
                    "were already finishing",
                    "have already finished"
                ),
                correctAnswer = 1,
                explanation = "The past perfect ('had already finished') is used to show that one past action occurred before another past action."
            )
        ),
        LearningChapter(
            id = "GE_C3",
            title = "Chapter 3: Articles, Determiners & Quantifiers",
            subtitle = "Nuances and Generic vs Specific Reference",
            coreConcept = "The zero article is used with plurals and uncountable nouns when making general statements. Special nuances apply with geographical names, unique roles, and generic singulars (e.g., 'the computer has changed society'). Quantifiers like 'few/little' vs 'a few/a little' express positive or negative bias.",
            vocabulary = listOf(
                "Zero article" to "The absence of 'a', 'an', or 'the' before a noun.",
                "Generic reference" to "Using a noun to represent an entire class or species rather than one specific item.",
                "Determiner" to "A word placed before a noun to clarify what the noun refers to."
            ),
            quiz = InteractiveQuiz(
                question = "What is the difference between 'few' and 'a few'?",
                options = listOf(
                    "No difference, they are completely interchangeable.",
                    "'Few' has a negative connotation ('almost none'), while 'a few' is positive ('some').",
                    "'Few' is used for uncountable nouns and 'a few' for countable.",
                    "'Few' is formal and 'a few' is informal."
                ),
                correctAnswer = 1,
                explanation = "'Few' suggests scarcity or lack, whereas 'a few' indicates a positive, sufficient count of items."
            )
        ),
        LearningChapter(
            id = "GE_C4",
            title = "Chapter 4: Prepositions & Phrasal Verbs",
            subtitle = "Contextual & High-Frequency Combinations",
            coreConcept = "Phrasal verbs are multi-word combinations of a verb and a particle (preposition or adverb) that create idiomatic meanings. Context dictates whether phrasal verbs are separable or inseparable, and formal vs informal. Prepositions of time, place, and direction also have abstract, professional usages.",
            vocabulary = listOf(
                "Phrasal verb" to "An idiomatic phrase consisting of a verb and another element (preposition or adverb).",
                "Separable" to "Phrasal verbs where the direct object can go between the verb and the particle.",
                "Inseparable" to "Phrasal verbs where the particle must stay directly next to the verb."
            ),
            quiz = InteractiveQuiz(
                question = "Identify the phrasal verb that means 'to tolerate or bear a difficult situation':",
                options = listOf(
                    "Put up with",
                    "Look up to",
                    "Take after",
                    "Bring about"
                ),
                correctAnswer = 0,
                explanation = "'Put up with' is a high-frequency phrasal verb meaning to tolerate patiently."
            )
        ),
        LearningChapter(
            id = "GE_C5",
            title = "Chapter 5: Modals & Conditionals",
            subtitle = "Advanced, Mixed, Inverted and Subjunctive",
            coreConcept = "Conditionals express hypothetical, imaginary, or factual situations. Advanced conditionals include mixed types (combining past conditions with present results) and inverted conditionals (e.g., 'Had I known, I would have...') which omit 'if'. Modals express shades of obligation, permission, and logical deduction.",
            vocabulary = listOf(
                "Mixed conditional" to "A conditional sentence that refers to a past condition with a present result.",
                "Conditional inversion" to "A formal structure that replaces 'if' with auxiliary verbs ('Had I...', 'Were you to...').",
                "Subjunctive" to "A verb mood used to express wishes, demands, or suggestions (e.g., 'I insist that he be present')."
            ),
            quiz = InteractiveQuiz(
                question = "Which is the correct inverted form of 'If I had been informed, I would have attended.'?",
                options = listOf(
                    "If had I been informed, I would have attended.",
                    "Had I been informed, I would have attended.",
                    "Should I have been informed, I would have attended.",
                    "Were I to be informed, I would have attended."
                ),
                correctAnswer = 1,
                explanation = "'Had I been informed' is the correct past inversion, omitting 'if' and reversing the subject and auxiliary verb."
            )
        ),
        LearningChapter(
            id = "GE_C6",
            title = "Chapter 6: Voice (Active-Passive)",
            subtitle = "Advanced Passive and Causatives",
            coreConcept = "The passive voice is vital in academic and professional writing to emphasize the action over the agent. Advanced structures include passive with modals (e.g., 'must be completed') and causative structures (e.g., 'get/have something done') to represent delegating tasks.",
            vocabulary = listOf(
                "Causative" to "A grammatical structure indicating that someone causes another person to perform an action.",
                "Agent" to "The person or thing performing the action in a sentence.",
                "Passive modal" to "A passive construction containing a modal verb (e.g., 'should be finished')."
            ),
            quiz = InteractiveQuiz(
                question = "Which of the following represents a causative structure?",
                options = listOf(
                    "The proposal was submitted yesterday.",
                    "We had the developers rebuild the database.",
                    "The developers rebuilt the database in two days.",
                    "The database is being rebuilt right now."
                ),
                correctAnswer = 1,
                explanation = "'Had the developers rebuild' uses the causative verb 'have' to indicate delegating an action."
            )
        ),
        LearningChapter(
            id = "GE_C7",
            title = "Chapter 7: Reported Speech",
            subtitle = "Advanced Reporting Verbs & Exceptions",
            coreConcept = "Reported speech shifts the speaker's original words back in tense (backshift). Advanced reporting verbs (e.g., 'advocate', 'concede', 'reiterate') convey nuance and replace simple reporting verbs like 'said'. Tense backshift exceptions apply when reporting facts that are still true.",
            vocabulary = listOf(
                "Backshift" to "The process of shifting tenses backward in reported speech.",
                "Reporting verb" to "A verb used to report someone else's speech, indicating tone or intention.",
                "Concede" to "To admit that something is true or valid after first denying or resisting it."
            ),
            quiz = InteractiveQuiz(
                question = "When reporting 'I live in Paris' when the statement remains true today, tense backshift is:",
                options = listOf(
                    "Mandatory (must use past tense).",
                    "Optional, since the fact is still active and true.",
                    "Incorrect (must always stay present).",
                    "Only allowed with past continuous."
                ),
                correctAnswer = 1,
                explanation = "Backshifting is optional if the reported statement is still true or relevant at the time of reporting."
            )
        ),
        LearningChapter(
            id = "GE_C8",
            title = "Chapter 8: Clauses & Connectors",
            subtitle = "Relative, Noun, Adverb Clauses & Linking Devices",
            coreConcept = "Clauses are the building blocks of cohesive writing. Relative clauses add detail, noun clauses act as subjects or objects, and adverb clauses express cause, contrast, or condition. Advanced connectors (e.g., 'notwithstanding', 'consequently') link ideas across sentences smoothly.",
            vocabulary = listOf(
                "Relative clause" to "A clause that starts with a relative pronoun to define or modify a noun.",
                "Noun clause" to "A dependent clause that functions as a noun in a sentence.",
                "Notwithstanding" to "An advanced preposition/adverb meaning 'in spite of' or 'despite'."
            ),
            quiz = InteractiveQuiz(
                question = "Which connector fits best: 'The strategy failed, ______ we learned invaluable lessons.'?",
                options = listOf(
                    "consequently",
                    "nevertheless",
                    "subsequently",
                    "furthermore"
                ),
                correctAnswer = 1,
                explanation = "'Nevertheless' is a high-level contrast connector meaning 'in spite of that'."
            )
        ),
        LearningChapter(
            id = "GE_C9",
            title = "Chapter 9: Thematic Vocabulary",
            subtitle = "Academic Word List, Tech & Business",
            coreConcept = "Expanding vocabulary beyond basic verbs is essential for high-level speaking and writing. This chapter targets vocabulary from the Academic Word List (AWL), emerging technology fields (AI, automation), and corporate business strategies (scalability, alignment).",
            vocabulary = listOf(
                "Disruptive" to "Innovative technology that completely displaces established products.",
                "Synergy" to "The interaction or cooperation of two or more organizations to produce a combined effect greater than the sum of their separate effects.",
                "Paradigm shift" to "A fundamental change in approach or underlying assumptions."
            ),
            quiz = InteractiveQuiz(
                question = "What does a 'paradigm shift' refer to in a technical context?",
                options = listOf(
                    "A routine update in software code.",
                    "A fundamental, revolutionary change in how things are done.",
                    "A transition to a different office layout.",
                    "A reduction in database size."
                ),
                correctAnswer = 1,
                explanation = "A paradigm shift is a profound change in core methodologies or systems."
            )
        ),
        LearningChapter(
            id = "GE_C10",
            title = "Chapter 10: Collocations, Synonyms & Antonyms",
            subtitle = "High-Level Corpus-Based Combinations",
            coreConcept = "Collocations are word pairings that sound natural to native speakers (e.g., 'mitigate risk' rather than 'lessen risk'). Synonyms and antonyms add variety to your speech, allowing you to shift registers and express shades of meaning with precision.",
            vocabulary = listOf(
                "Collocation" to "The habitual juxtaposition of a particular word with another word or words with a frequency greater than chance.",
                "Mitigate" to "Make less severe, serious, or painful.",
                "Nuanced" to "Characterized by subtle shades of meaning or expression."
            ),
            quiz = InteractiveQuiz(
                question = "Which collocation is most natural in professional English?",
                options = listOf(
                    "Do a decision",
                    "Make a decision",
                    "Take a decision",
                    "Formulate a decision"
                ),
                correctAnswer = 1,
                explanation = "'Make a decision' (or 'take a decision' in UK English) is the standard natural collocation."
            )
        ),
        LearningChapter(
            id = "GE_C11",
            title = "Chapter 11: Idioms, Proverbs & Expressions",
            subtitle = "Contextual Advanced Figurative English",
            coreConcept = "Idiomatic expressions add flavor and color to informal and semi-formal English. High-level idioms (e.g., 'hit the nail on the head', 'burn the midnight oil') must be used sparingly and in correct contextual registers to sound natural and persuasive.",
            vocabulary = listOf(
                "Idiom" to "A group of words established by usage as having a meaning not deducible from those of the individual words.",
                "Burn the midnight oil" to "To read or work late into the night.",
                "Hit the nail on the head" to "To describe exactly what is causing a situation or problem."
            ),
            quiz = InteractiveQuiz(
                question = "What does 'burn the midnight oil' mean?",
                options = listOf(
                    "To waste heating resources unnecessarily.",
                    "To work or study late into the night.",
                    "To start a kitchen fire accidentally.",
                    "To finish a project ahead of schedule."
                ),
                correctAnswer = 1,
                explanation = "Burning the midnight oil historically referred to using oil lamps to work late into the night."
            )
        ),
        LearningChapter(
            id = "GE_C12",
            title = "Chapter 12: One-Word Substitutions & Homophones",
            subtitle = "Professional & Literary Precision",
            coreConcept = "One-word substitutions compress wordy descriptions into single, professional terms (e.g., 'unprecedented' for 'never happened before'). Homophones (words with the same sound but different spellings and meanings, e.g., 'complement' vs 'compliment') must be distinguished to avoid errors.",
            vocabulary = listOf(
                "Homophone" to "Two or more words having the same pronunciation but different meanings, origins, or spelling.",
                "Complement" to "Something that completes or brings to perfection.",
                "Compliment" to "A polite expression of praise or admiration."
            ),
            quiz = InteractiveQuiz(
                question = "Which word fits: 'The new graphic design is a perfect ______ to our website layout.'?",
                options = listOf(
                    "compliment",
                    "complement",
                    "complament",
                    "complimentation"
                ),
                correctAnswer = 1,
                explanation = "'Complement' means it completes the layout beautifully, whereas 'compliment' refers to praise."
            )
        ),
        LearningChapter(
            id = "GE_C13",
            title = "Chapter 13: Advanced Reading Comprehension",
            subtitle = "Inference, Tone & Author’s Purpose",
            coreConcept = "Advanced reading comprehension requires understanding not just the explicit words, but also implicit meanings. Readers must infer the author's tone, identify rhetorical purposes, and practice speed-reading strategies like skimming and scanning.",
            vocabulary = listOf(
                "Inference" to "A conclusion reached on the basis of evidence and reasoning rather than explicit statements.",
                "Tone" to "The general character or attitude of a place, piece of writing, or situation.",
                "Skimming" to "Reading rapidly in order to get a general overview of the material."
            ),
            quiz = InteractiveQuiz(
                question = "If an author writes with high sarcasm, the tone is likely described as:",
                options = listOf(
                    "Objective",
                    "Sardonic",
                    "Whimsical",
                    "Earnest"
                ),
                correctAnswer = 1,
                explanation = "'Sardonic' refers to grimly mocking or cynical, which aligns perfectly with heavy sarcasm."
            )
        ),
        LearningChapter(
            id = "GE_C14",
            title = "Chapter 14: Listening Mastery",
            subtitle = "Note-Taking, Lectures & Accents",
            coreConcept = "Listening in academic or professional settings requires tracking arguments, taking structured notes, and understanding different English accents (British, American, Australian, Indian) without losing comprehension.",
            vocabulary = listOf(
                "Active listening" to "Fully concentrating on what is being said rather than just passively 'hearing' the speaker.",
                "Linguistic accent" to "A distinctive mode of pronunciation of a language, especially associated with a particular nation or locality.",
                "Shorthand" to "A method of rapid writing using abbreviations and symbols, useful for note-taking."
            ),
            quiz = InteractiveQuiz(
                question = "Which active listening strategy is most effective during a lecture?",
                options = listOf(
                    "Attempting to write down every single word spoken.",
                    "Recording key nouns, verbs, linking terms, and drawing logical connections.",
                    "Closing your eyes and ignoring visual cues completely.",
                    "Only writing down things that you disagree with."
                ),
                correctAnswer = 1,
                explanation = "Taking structured, semantic notes focusing on key verbs, nouns, and relations boosts retention and comprehension."
            )
        ),
        LearningChapter(
            id = "GE_C15",
            title = "Chapter 15: Advanced Writing Skills",
            subtitle = "Cohesion, Coherence & Essay Structures",
            coreConcept = "High-level writing must exhibit cohesive devices (pronouns, conjunctions, transition words) and clear paragraph coherence (each paragraph centering on one clear theme). It teaches structures for argumentative, balanced, and descriptive essays.",
            vocabulary = listOf(
                "Cohesion" to "The grammatical and lexical linking within a text that holds it together.",
                "Coherence" to "The logical order and connection of ideas so that a text makes complete sense.",
                "Topic sentence" to "A sentence that expresses the main idea of the paragraph in which it occurs."
            ),
            quiz = InteractiveQuiz(
                question = "What is the primary role of a 'topic sentence' in an essay paragraph?",
                options = listOf(
                    "To conclude the essay with a memorable quote.",
                    "To state the main idea and scope of that specific paragraph.",
                    "To list all the sources and citations used.",
                    "To ask the reader a direct rhetorical question."
                ),
                correctAnswer = 1,
                explanation = "A topic sentence sets the expectation and outlines the single central theme of the paragraph."
            )
        ),
        LearningChapter(
            id = "GE_C16",
            title = "Chapter 16: Gerunds, Infinitives & Participles",
            subtitle = "Advanced Verbal Rules & Error Prevention",
            coreConcept = "Gerunds (verb+ing acting as nouns) and infinitives (to+verb) follow strict rules of verb-complementation (e.g., 'suggest doing' vs 'agree to do'). Participles act as adjectives or form participial clauses to compress sentences.",
            vocabulary = listOf(
                "Gerund" to "A form that is derived from a verb but functions as a noun, ending in -ing.",
                "Infinitive" to "The basic form of a verb, usually preceded by 'to'.",
                "Participial clause" to "A clause containing a present or past participle, used to compress information (e.g., 'Having finished the report, she left')."
            ),
            quiz = InteractiveQuiz(
                question = "Which sentence is grammatically correct?",
                options = listOf(
                    "She suggested to go to the meeting.",
                    "She suggested going to the meeting.",
                    "She suggested for going to the meeting.",
                    "She suggested go to the meeting."
                ),
                correctAnswer = 1,
                explanation = "The verb 'suggest' must be complemented by a gerund ('going'), not an infinitive."
            )
        ),
        LearningChapter(
            id = "GE_C17",
            title = "Chapter 17: Punctuation & Stylistics",
            subtitle = "Advanced Punctuation & Rhetoric Devices",
            coreConcept = "Punctuation is the roadmap of written English. Semicolons link independent clauses, colons introduce lists or explanations, and dashes add emphatic parenthetical detail. Rhetorical devices (e.g., alliteration, parallelism) add style.",
            vocabulary = listOf(
                "Semicolon" to "A punctuation mark (;) indicating a pause, typically between two main clauses, that is more pronounced than that indicated by a comma.",
                "Rhetorical device" to "A technique that an author or speaker uses to convey a meaning with the goal of persuading.",
                "Em-dash" to "A long dash (—) used to set off a parenthetical element or a sudden break in thought."
            ),
            quiz = InteractiveQuiz(
                question = "When is it most appropriate to use a semicolon?",
                options = listOf(
                    "To link two closely related independent clauses without a coordinating conjunction.",
                    "To introduce a very long numbered shopping list.",
                    "To conclude a question instead of a question mark.",
                    "To separate a verb from its direct object."
                ),
                correctAnswer = 0,
                explanation = "Semicolons are ideal for linking related independent clauses that could stand as separate sentences but share a close logical tie."
            )
        ),
        LearningChapter(
            id = "GE_C18",
            title = "Chapter 18: Common Indian Errors & MTI",
            subtitle = "MTI Remediation & Accent Neutralization",
            coreConcept = "Many Indian speakers experience MTI (Mother Tongue Influence) affecting vowel/consonant pronunciations and make typical grammatical errors (e.g., 'discussing about', using 'only' as an intensifier). Remediation improves global clarity.",
            vocabulary = listOf(
                "MTI" to "Mother Tongue Influence; pronunciation or grammatical traits carried over from a native language.",
                "Redundancy" to "The use of words that could be omitted without loss of meaning (e.g., 'discussing about').",
                "Intensifier" to "An adverb used to give force or emphasis (e.g., 'only', 'very')."
            ),
            quiz = InteractiveQuiz(
                question = "Identify the grammatically redundant phrase commonly used in MTI errors:",
                options = listOf(
                    "Let's discuss the budget proposal.",
                    "Let's discuss about the budget proposal.",
                    "Let's talk about the budget proposal.",
                    "Let's review the budget proposal."
                ),
                correctAnswer = 1,
                explanation = "The verb 'discuss' already means 'talk about', so adding 'about' is redundant."
            )
        ),
        LearningChapter(
            id = "GE_C19",
            title = "Chapter 19: Phonetics & Pronunciation",
            subtitle = "IPA, Word Stress & Minimal Pairs",
            coreConcept = "Phonetics uses the International Phonetic Alphabet (IPA) to standardize sounds. Word stress (vocal emphasis on specific syllables) is crucial for comprehension, and minimal pairs (words differing by only one sound, e.g., 'ship' vs 'sheep') prevent confusion.",
            vocabulary = listOf(
                "IPA" to "International Phonetic Alphabet; an alphabetic system of phonetic notation.",
                "Word stress" to "The emphasis placed on a specific syllable within a word.",
                "Minimal pair" to "Two words that differ in pronunciation by only one single sound (e.g., 'think' and 'sink')."
            ),
            quiz = InteractiveQuiz(
                question = "Where is the primary word stress placed in the word 'DEVELOPER'?",
                options = listOf(
                    "On the first syllable: DE-veloper",
                    "On the second syllable: de-VEL-oper",
                    "On the third syllable: devel-OP-er",
                    "On the final syllable: develop-ER"
                ),
                correctAnswer = 1,
                explanation = "'Developer' has its primary stress on the second syllable: de-VEL-op-er."
            )
        ),
        LearningChapter(
            id = "GE_C20",
            title = "Chapter 20: Intonation, Rhythm & Connected Speech",
            subtitle = "Weak Forms, Linking & Elision",
            coreConcept = "English is a stress-timed language where unstressed syllables compress into weak forms (using the schwa sound). Connected speech features linking (joining words smoothly) and elision (disappearing sounds) to achieve natural rhythm.",
            vocabulary = listOf(
                "Stress-timed" to "A language rhythm where stressed syllables occur at relatively regular intervals, regardless of unstressed syllables.",
                "Schwa" to "The unstressed, central vowel sound represented by the symbol /ə/, the most common sound in English.",
                "Elision" to "The omission of a sound or syllable when speaking (e.g., 'next door' sounding like 'nex-door')."
            ),
            quiz = InteractiveQuiz(
                question = "What is the primary sound of the schwa /ə/ in connected speech?",
                options = listOf(
                    "A long, open 'ah' sound.",
                    "A very short, relaxed, neutral 'uh' sound.",
                    "A high-pitched 'ee' sound.",
                    "A sharp, distinct 'oh' sound."
                ),
                correctAnswer = 1,
                explanation = "The schwa /ə/ is the most common weak vowel sound in English, representing a relaxed, neutral, unstressed vocal position."
            )
        ),
        LearningChapter(
            id = "GE_C21",
            title = "Chapter 21: Conversational English",
            subtitle = "Discourse Markers & Fillers",
            coreConcept = "Fluent conversation relies on natural discourse markers (e.g., 'conversely', 'mind you') to signal transitions, and polite fillers (e.g., 'well', 'actually') to handle turn-taking and avoid awkward silences.",
            vocabulary = listOf(
                "Discourse marker" to "A word or phrase that manages the flow and structure of discourse (e.g., 'by the way', 'on the other hand').",
                "Filler" to "Sounds or words like 'um', 'ah', 'like', or 'you know' used to fill pauses in speech.",
                "Turn-taking" to "The social mechanism where speakers coordinate who is talking and when."
            ),
            quiz = InteractiveQuiz(
                question = "Which of these is a professional discourse marker used to introduce a contrasting point?",
                options = listOf(
                    "Additionally",
                    "Consequently",
                    "Conversely",
                    "Furthermore"
                ),
                correctAnswer = 2,
                explanation = "'Conversely' is a C1-level discourse marker used to introduce an idea that is opposite or contrasting."
            )
        ),
        LearningChapter(
            id = "GE_C22",
            title = "Chapter 22: Formal vs Informal Register",
            subtitle = "Style Shifting & Politeness",
            coreConcept = "Choosing the correct register is essential. This chapter teaches style shifting—switching between formal language for meetings or reports and informal, casual language for social coffee breaks, maintaining politeness strategies.",
            vocabulary = listOf(
                "Register" to "The scale of formality or style in language determined by context, topic, and relationship.",
                "Style shifting" to "Adjusting language registers to match different settings or social cues.",
                "Euphemism" to "A mild or indirect word substituted for one considered to be too harsh or blunt."
            ),
            quiz = InteractiveQuiz(
                question = "Which request is most appropriate in a formal executive board meeting?",
                options = listOf(
                    "Shut the window, it's freezing in here.",
                    "Would you mind if we closed the window? It seems rather cold.",
                    "I want the window closed immediately.",
                    "Let's close the window, okay?"
                ),
                correctAnswer = 1,
                explanation = "Using 'Would you mind if...' establishes a polite, highly respectful formal register suited for professional boards."
            )
        ),
        LearningChapter(
            id = "GE_C23",
            title = "Chapter 23: Storytelling & Descriptive Writing",
            subtitle = "Advanced Narrative Techniques",
            coreConcept = "Storytelling is an effective way to connect with listeners. Techniques like sensory descriptions, narrative hooks, and chronological pacing make both verbal stories and descriptive essays engaging and unforgettable.",
            vocabulary = listOf(
                "Narrative hook" to "An opening statement designed to capture the reader's attention instantly.",
                "Sensory description" to "Imagery that appeals to the five human senses (sight, sound, smell, taste, touch).",
                "Pacing" to "The speed and cadence at which a narrative unfolds."
            ),
            quiz = InteractiveQuiz(
                question = "Which of these functions as a strong narrative hook?",
                options = listOf(
                    "This essay will discuss why climate change is bad.",
                    "It was a freezing Tuesday when the entire grid went completely silent.",
                    "There are many arguments about the economy.",
                    "I am writing to tell you a story about my life."
                ),
                correctAnswer = 1,
                explanation = "Stating 'when the entire grid went completely silent' sparks mystery, immediate interest, and acts as an excellent narrative hook."
            )
        ),
        LearningChapter(
            id = "GE_C24",
            title = "Chapter 24: Professional & Academic Vocabulary",
            subtitle = "Lexical Chunks & C1-C2 Terminology",
            coreConcept = "To reach native-level fluency, speakers must use lexical chunks (pre-assembled phrases, e.g., 'with the utmost respect') and high-level C1-C2 terms to discuss complex political, scientific, or technological topics smoothly.",
            vocabulary = listOf(
                "Lexical chunk" to "A group of words that are commonly found together, stored as a single unit in memory.",
                "Sovereignty" to "Supreme power or authority; complete independence.",
                "Ubiquitous" to "Present, appearing, or found everywhere."
            ),
            quiz = InteractiveQuiz(
                question = "What does the word 'ubiquitous' mean?",
                options = listOf(
                    "Extremely rare or difficult to locate.",
                    "Existing or appearing everywhere at the same time.",
                    "Fragile and prone to breaking.",
                    "Confusing or complicated."
                ),
                correctAnswer = 1,
                explanation = "'Ubiquitous' means ever-present or widespread, describing technologies like smartphones or AI today."
            )
        )
    )

    val interviewPrepList = listOf(
        LearningChapter(
            id = "IP_U1",
            title = "Unit 1: Self-Introduction Mastery",
            subtitle = "Perfecting the Present-Past-Future Pitch",
            coreConcept = "Your introduction should follow the Present-Past-Future framework. Start with your current role and major achievements. Next, touch briefly on past key milestones. Conclude by expressing why you are excited about this specific prospective role.",
            vocabulary = listOf(
                "Value proposition" to "A promise of value to be delivered, communicated, and acknowledged.",
                "Spearhead" to "To lead an initiative or team project confidently.",
                "Elevator pitch" to "A succinct, persuasive sales pitch or personal summary."
            ),
            quiz = InteractiveQuiz(
                question = "Which structure is recommended for a highly compelling job interview self-introduction?",
                options = listOf(
                    "List every job from childhood to present in chronological order.",
                    "The Present-Past-Future framework: current status, key highlights, and future interest.",
                    "Only talk about personal hobbies and family background.",
                    "Ask the interviewer to read your resume instead of speaking."
                ),
                correctAnswer = 1,
                explanation = "The Present-Past-Future framework keeps your introduction structured, highly professional, and relevant to the target role."
            )
        ),
        LearningChapter(
            id = "IP_U2",
            title = "Unit 2: Behavioral Interview Techniques",
            subtitle = "Nailing the STAR & CAR Methodology",
            coreConcept = "Behavioral questions probe your past experiences. Structure your answers using the STAR (Situation, Task, Action, Result) or CAR (Context, Action, Result) method. Focus 70% of your response on your specific Action and the quantifiable Result.",
            vocabulary = listOf(
                "Quantifiable" to "Capable of being measured or expressed as a numerical value.",
                "STAR method" to "Situation, Task, Action, Result framework for behavioral interview questions.",
                "CAR method" to "Context, Action, Result; a streamlined variation of the STAR method."
            ),
            quiz = InteractiveQuiz(
                question = "When answering behavioral questions, where should you spend the majority of your time?",
                options = listOf(
                    "Describing the office layout and team size.",
                    "Explaining the specific actions you took and the measurable results achieved.",
                    "Complaining about the previous management structure.",
                    "Highlighting how difficult the technical requirements were."
                ),
                correctAnswer = 1,
                explanation = "Interviewers assess your problem-solving capability based on your explicit actions and how they drove concrete results."
            )
        ),
        LearningChapter(
            id = "IP_U3",
            title = "Unit 3: Technical & Role-Specific Interviews",
            subtitle = "Tech, Management & Leadership Insights",
            coreConcept = "Technical and leadership interviews assess not just coding, but structural problem-solving and mentoring. Standard framework: Clarify constraints, propose multiple options, weigh trade-offs, and detail implementation steps.",
            vocabulary = listOf(
                "Trade-off" to "A compromise between two desirable but conflicting features.",
                "Bottleneck" to "A point of congestion in a system that slows down progress.",
                "Technical debt" to "The implied cost of additional rework caused by choosing an easy solution now instead of a better approach."
            ),
            quiz = InteractiveQuiz(
                question = "How should you start answering a complex technical or system design question?",
                options = listOf(
                    "Immediately start writing code or drawing blocks without speaking.",
                    "Clarify requirements, define scale boundaries, and establish core assumptions.",
                    "Say that the question is too hard or unrealistic.",
                    "Ask the interviewer to solve the first part for you."
                ),
                correctAnswer = 1,
                explanation = "Defining parameters and clarifying constraints first shows robust analytical, software-architect methodology."
            )
        ),
        LearningChapter(
            id = "IP_U4",
            title = "Unit 4: Tricky Questions Handling",
            subtitle = "Gaps, Weakness, Failures & 'Why Us?'",
            coreConcept = "Tricky questions test self-awareness and integrity. Frame weaknesses as areas of active, structured self-improvement. Explain resume gaps with productive sabbaticals, and research company core values thoroughly to deliver a unique 'Why Us' pitch.",
            vocabulary = listOf(
                "Self-awareness" to "Conscious knowledge of one's own character, feelings, motives, and desires.",
                "Sabbatical" to "A period of paid or unpaid leave for study, travel, or upskilling.",
                "Growth mindset" to "The belief that abilities can be developed through dedication and hard work."
            ),
            quiz = InteractiveQuiz(
                question = "What is the best way to discuss a previous failure in an interview?",
                options = listOf(
                    "Deny that you have ever failed in any project.",
                    "State a genuine professional mistake, explain what you learned, and how you prevent it now.",
                    "Blame another team member for the setback.",
                    "Tell a trivial, non-professional story from childhood."
                ),
                correctAnswer = 1,
                explanation = "Showing a genuine professional mistake and documenting the learning loop displays maturity, growth mindset, and high accountability."
            )
        ),
        LearningChapter(
            id = "IP_U5",
            title = "Unit 5: Negotiation & HR Round",
            subtitle = "Salary, Perks & Work-Life Balance Integration",
            coreConcept = "Negotiation is a collaborative process. Research industry benchmarks on sites like Glassdoor and levels.fyi. Express high enthusiasm for the role before initiating negotiations, and negotiate base salary, signing bonus, and equity as a complete package.",
            vocabulary = listOf(
                "Benchmark" to "A standard or point of reference against which things may be compared.",
                "Equity" to "Stock options or shares of ownership in a company offered as part of compensation.",
                "Base salary" to "The fixed amount of money paid to an employee before any bonuses or benefits."
            ),
            quiz = InteractiveQuiz(
                question = "What is the recommended approach when an HR recruiter asks for your salary expectations?",
                options = listOf(
                    "State a single, rigid number and refuse to negotiate.",
                    "Provide a researched, flexible range based on industry benchmarks and total packaging.",
                    "Demand to know the interviewer's own salary first.",
                    "Say you will work for free to show dedication."
                ),
                correctAnswer = 1,
                explanation = "Providing a flexible, benchmark-backed range maintains a collaborative tone while protecting your financial market value."
            )
        ),
        LearningChapter(
            id = "IP_U6",
            title = "Unit 6: Full Mock Interviews",
            subtitle = "Multiple-Round Prep & Stamina",
            coreConcept = "A typical recruitment process includes Screening, Technical Case, and a final Executive cultural fit round. Prepare for each by practicing voice clarity, pacing, and concise summaries. Rehearsing in real-time is vital.",
            vocabulary = listOf(
                "Culture fit" to "The alignment of a candidate's values, beliefs, and behaviors with those of the hiring organization.",
                "Stamina" to "The ability to sustain prolonged physical or mental effort.",
                "Screening" to "A brief initial conversation to assess basic qualifications and interest."
            ),
            quiz = InteractiveQuiz(
                question = "What is the primary objective of a 'cultural fit' round?",
                options = listOf(
                    "To test your ability to write perfect, bug-free algorithms.",
                    "To assess if your working style, ethics, and values align with the team.",
                    "To check if you are willing to work 24 hours a day.",
                    "To inspect your college academic transcripts."
                ),
                correctAnswer = 1,
                explanation = "Cultural fit rounds ensure that you will communicate constructively and thrive within the existing team dynamics."
            )
        ),
        LearningChapter(
            id = "IP_U7",
            title = "Unit 7: Post-Interview & Confidence Building",
            subtitle = "Follow-ups, Body Language & Mindset",
            coreConcept = "Maximize your odds after the interview by sending a personalized thank-you email within 24 hours, reiterating a specific key topic discussed. During calls, maintain open body language, a natural smile, and deep, calm breathing.",
            vocabulary = listOf(
                "Rapport" to "A close and harmonious relationship in which the people understand each other's feelings.",
                "Body language" to "Non-verbal signals that we use to communicate our feelings and intentions.",
                "Mindset" to "The established set of attitudes held by someone."
            ),
            quiz = InteractiveQuiz(
                question = "A great post-interview thank-you email should be sent within:",
                options = listOf(
                    "10 minutes.",
                    "24 hours, mentioning a unique, memorable point from your conversation.",
                    "One week.",
                    "Only after you receive an offer or rejection."
                ),
                correctAnswer = 1,
                explanation = "Sending a tailored thank-you within 24 hours demonstrates promptness, professionalism, and reinforces rapport."
            )
        )
    )

    val officeEnglishList = listOf(
        LearningChapter(
            id = "OE_U1",
            title = "Unit 1: Professional Email Writing",
            subtitle = "Structure, Tone, Clarity & Follow-ups",
            coreConcept = "Effective emails are brief, structured, and action-oriented. Always state a clear, concise subject line. Break up long paragraphs with bullet points, and close with a clear call-to-action (CTA).",
            vocabulary = listOf(
                "Succinct" to "Briefly and clearly expressed (especially of written or spoken words).",
                "Action-oriented" to "Focused on taking action or getting specific results.",
                "Salutation" to "A gesture of greeting; a polite expression of welcome."
            ),
            quiz = InteractiveQuiz(
                question = "A well-written business email should always conclude with a:",
                options = listOf(
                    "Long explanation of why you wrote the email.",
                    "Clear, actionable Call-to-Action (CTA).",
                    "Humorous joke to lighten the atmosphere.",
                    "Series of unrelated attachments."
                ),
                correctAnswer = 1,
                explanation = "Ending with a clear Call-to-Action (CTA) ensures the recipient knows exactly what step to take next."
            )
        ),
        LearningChapter(
            id = "OE_U2",
            title = "Unit 2: Effective Meetings",
            subtitle = "Participation, Chairing, Agenda & Minutes",
            coreConcept = "Meetings require structure. The facilitator (chair) sets an agenda beforehand. Participants contribute constructively by using transitions like 'Building on that...' or 'If I may raise a point...'. A summary of key decisions (minutes) is circulated afterward.",
            vocabulary = listOf(
                "Agenda" to "A list of items to be discussed at a formal meeting.",
                "Facilitator" to "A person who guides a discussion or meeting to make it easier for people to reach agreement.",
                "Minutes" to "The official written record of what was discussed and decided in a meeting."
            ),
            quiz = InteractiveQuiz(
                question = "What is the main purpose of meeting 'minutes'?",
                options = listOf(
                    "To record the exact second the meeting started.",
                    "To summarize key discussion points, decisions, and action items for transparency.",
                    "To list all the complaints team members made.",
                    "To decide who gets a promotion."
                ),
                correctAnswer = 1,
                explanation = "Minutes serve as the official action roadmap, documenting ownership and timelines for decisions made."
            )
        ),
        LearningChapter(
            id = "OE_U3",
            title = "Unit 3: Powerful Presentations",
            subtitle = "Structuring, Visual Slide Prep & Q&A Mastery",
            coreConcept = "Capture attention using the Hook-Problem-Solution-Value structure. Restrict slides to key phrases (avoid walls of text), and handle difficult Q&A questions by validating the query: 'That is an excellent point, let's look at...'.",
            vocabulary = listOf(
                "Onboarding" to "The process of integrating a new employee or user into an organization or platform.",
                "Retention" to "The continued possession, use, or control of something (e.g., customer retention).",
                "Validation" to "The action of checking or proving the validity or accuracy of something."
            ),
            quiz = InteractiveQuiz(
                question = "What should you do if an audience member asks a question you don't know the answer to?",
                options = listOf(
                    "Make up a random answer on the spot to save face.",
                    "Acknowledge the question, validate its importance, and offer to follow up offline with accurate data.",
                    "Ignore the person and move to the next slide.",
                    "Argue with them about the relevance of the question."
                ),
                correctAnswer = 1,
                explanation = "Validating the query and offering to follow up offline maintains high credibility and professional integrity."
            )
        ),
        LearningChapter(
            id = "OE_U4",
            title = "Unit 4: Telephonic & Video Call Mastery",
            subtitle = "Clarity, Professional Politeness & Technical Handling",
            coreConcept = "Remote calls demand clear articulation, checking for audio clarity ('Can everyone hear me clearly?'), and utilizing polite interruptions: 'Pardon the interruption, but could we clarify...'.",
            vocabulary = listOf(
                "Articulation" to "The formation of clear and distinct sounds in speech.",
                "Interjection" to "An abrupt remark, or polite interruption made in speech.",
                "Bandwidth" to "The transmission capacity of a computer network connection, or personal mental availability."
            ),
            quiz = InteractiveQuiz(
                question = "How should you politely interrupt someone during a video call to add a vital point?",
                options = listOf(
                    "Start speaking loudly over their voice.",
                    "Say: 'Pardon the interruption, but may I add a brief, relevant update here?'",
                    "Mute their microphone using administrator controls.",
                    "Text them a harsh message in the group chat."
                ),
                correctAnswer = 1,
                explanation = "Using polite verbal framing like 'Pardon the interruption...' ensures a respectful, professional conversational entry."
            )
        ),
        LearningChapter(
            id = "OE_U5",
            title = "Unit 5: Negotiation & Persuasion",
            subtitle = "Win-Win Framing & Objection Handling",
            coreConcept = "Persuasion relies on framing requests around mutual benefit (Win-Win). When faced with objections (e.g., budget limits), use the 'Feel, Felt, Found' framework or suggest trial periods to de-risk decisions.",
            vocabulary = listOf(
                "Persuasion" to "The action or fact of persuading someone or of being persuaded to do or believe something.",
                "Objection" to "An expression or feeling of disapproval or opposition.",
                "Win-Win" to "A negotiation outcome that benefits both parties involved."
            ),
            quiz = InteractiveQuiz(
                question = "What is the core philosophy of a 'Win-Win' negotiation?",
                options = listOf(
                    "To defeat the other party completely and take all resources.",
                    "To compromise so heavily that both parties are unhappy.",
                    "To identify solutions where both sides achieve their primary objectives.",
                    "To drag the negotiations out as long as possible."
                ),
                correctAnswer = 2,
                explanation = "Win-Win negotiations build sustainable partnerships by aligning incentives and delivering mutual value."
            )
        ),
        LearningChapter(
            id = "OE_U6",
            title = "Unit 6: Feedback & Difficult Conversations",
            subtitle = "Constructive Criticism & Conflict Resolution",
            coreConcept = "Deliver feedback using the Situation-Behavior-Impact (SBI) framework. Avoid personal attacks. Say: 'In yesterday's meeting, when you interrupted (behavior), it made the client hesitant (impact). Let's work on...'.",
            vocabulary = listOf(
                "Constructive" to "Serving a useful purpose; tending to build up.",
                "SBI framework" to "Situation, Behavior, and Impact; a objective feedback delivery method.",
                "Remediation" to "The action of remedying or correcting something."
            ),
            quiz = InteractiveQuiz(
                question = "Why is the Situation-Behavior-Impact (SBI) framework highly recommended?",
                options = listOf(
                    "It allows you to express your anger safely.",
                    "It focuses on objective actions and their measurable effects, removing subjective personal bias.",
                    "It is the fastest way to fire an underperforming worker.",
                    "It guarantees that the recipient will not feel sad."
                ),
                correctAnswer = 1,
                explanation = "SBI separates personal identity from behavioral actions, making feedback actionable and professional."
            )
        ),
        LearningChapter(
            id = "OE_U7",
            title = "Unit 7: Crisis Communication",
            subtitle = "Apology, Damage Control & Stakeholders",
            coreConcept = "When a business crisis occurs (e.g., service outage), communicate immediately. Follow the three steps: 1. Acknowledge and apologize, 2. Detail current action steps, 3. Propose long-term preventative measures.",
            vocabulary = listOf(
                "Mitigation" to "The action of reducing the severity, seriousness, or painfulness of something.",
                "Post-mortem" to "An analysis or discussion of an event after it is over, especially a technical failure.",
                "Stakeholder" to "A person with an interest or concern in something, especially a business."
            ),
            quiz = InteractiveQuiz(
                question = "What is the first step when communicating a critical software bug to an external client?",
                options = listOf(
                    "Blame a junior developer to deflect responsibility.",
                    "Acknowledge the bug transparently, apologize for the disruption, and outline the immediate fix strategy.",
                    "Ignore the client's support emails until the bug is fixed.",
                    "Pretend that everything is working perfectly."
                ),
                correctAnswer = 1,
                explanation = "Immediate transparency and ownership are essential to preserve client trust during a critical incident."
            )
        ),
        LearningChapter(
            id = "OE_U8",
            title = "Unit 8: Cross-Cultural Business English",
            subtitle = "Cultural Nuances & Global Etiquette",
            coreConcept = "Global teams have varying norms. High-context cultures rely on subtle indirect cues, while low-context cultures favor direct communication. Adapt by listening actively and clarifying intent politely.",
            vocabulary = listOf(
                "Nuance" to "A subtle difference in or shade of meaning, expression, or sound.",
                "Etiquette" to "The customary code of polite behavior in society or among members of a particular profession.",
                "Low-context" to "Communication styles that are highly direct, explicit, and clear."
            ),
            quiz = InteractiveQuiz(
                question = "In a low-context communication culture, what is highly valued?",
                options = listOf(
                    "Indirect hints and long silence.",
                    "Direct, clear, explicit statements of goals and expectations.",
                    "Formal hand gestures only.",
                    "Avoiding direct eye contact during presentations."
                ),
                correctAnswer = 1,
                explanation = "Low-context cultures (like Germany or the US) value clear, explicit directness in professional tasks."
            )
        ),
        LearningChapter(
            id = "OE_U9",
            title = "Unit 9: Reports & Proposals",
            subtitle = "Executive Summary & Data Representation",
            coreConcept = "Professional documents must be highly readable. Start with an Executive Summary summarizing the entire report. Use charts with clear labels and describe trends using verbs like 'surged', 'stabilized', or 'plummeted'.",
            vocabulary = listOf(
                "Executive Summary" to "A short document or section that summarizes a longer report or proposal.",
                "Volatility" to "Liability to change rapidly and unpredictably, especially for the worse.",
                "Projection" to "An estimate or forecast of a future situation or trend based on study of present data."
            ),
            quiz = InteractiveQuiz(
                question = "Where should the Executive Summary be located in a 50-page business proposal?",
                options = listOf(
                    "At the very end of the appendix.",
                    "At the very beginning, as the first major section.",
                    "Exactly in the middle of the document.",
                    "Omitted completely to save space."
                ),
                correctAnswer = 1,
                explanation = "Executives read the summary first to grasp key deliverables, making the very beginning the correct location."
            )
        ),
        LearningChapter(
            id = "OE_U10",
            title = "Unit 10: Client Acquisition & Management",
            subtitle = "Rapport Building & Closing Deals",
            coreConcept = "Client relationships are built on trust. Listen to client pain points first, match your services to those pain points, and close deals by proposing structured next steps: 'Shall we initiate the onboarding next Monday?'.",
            vocabulary = listOf(
                "Rapport" to "A harmonious relation or understanding between people.",
                "Pain point" to "A specific problem that prospective customers of your business are experiencing.",
                "Onboarding" to "The process of getting a client set up and integrated with your service or software."
            ),
            quiz = InteractiveQuiz(
                question = "What is the most effective way to address client hesitation during a sales pitch?",
                options = listOf(
                    "Demand that they sign the contract immediately.",
                    "Ask targeted questions to understand their specific concern, and address it with a tailored solution.",
                    "Ignore their hesitation and repeat the pricing model.",
                    "Offer to call their competitor instead."
                ),
                correctAnswer = 1,
                explanation = "Identifying and isolating the root of their concern demonstrates high empathy and results in a stronger partnership."
            )
        ),
        LearningChapter(
            id = "OE_U11",
            title = "Unit 11: Leadership & Team Communication",
            subtitle = "Motivation, Delegation & Mentoring",
            coreConcept = "Effective leaders inspire. Delegate tasks by stating clear goals, expectations, and support mechanisms. Use coaching language: 'How do you think we should approach this?' rather than 'Do it this way.'",
            vocabulary = listOf(
                "Delegation" to "The assignment of any authority or responsibility to another person to carry out specific activities.",
                "Mentorship" to "A professional relationship in which an experienced person assists another in developing skills.",
                "Empathy" to "The ability to understand and share the feelings of another."
            ),
            quiz = InteractiveQuiz(
                question = "When delegating a major project module, what should you clarify first?",
                options = listOf(
                    "That you will take all credit for the results.",
                    "The final objective, key deadlines, success metrics, and available support.",
                    "That they must not ask any questions.",
                    "What time they arrive at the office."
                ),
                correctAnswer = 1,
                explanation = "Delegation is most successful when outcomes, timelines, and boundaries are explicitly defined and supported."
            )
        ),
        LearningChapter(
            id = "OE_U12",
            title = "Unit 12: Advanced Business Vocabulary & Idioms",
            subtitle = "Corporate Jargon, Polite Phrases & Polish",
            coreConcept = "Polish your vocabulary by utilizing high-frequency corporate idioms and verbs (e.g., 'move the needle', 'touch base', 'leverage'). Ensure you maintain polite phrasing even in challenging situations.",
            vocabulary = listOf(
                "Move the needle" to "To make a significant, noticeable difference in a business metric.",
                "Touch base" to "Briefly make contact or consult with someone to update progress.",
                "Leverage" to "Use something to its maximum advantage."
            ),
            quiz = InteractiveQuiz(
                question = "What does 'move the needle' mean in a business context?",
                options = listOf(
                    "To adjust a physical pressure gauge in a machine.",
                    "To make a significant, measurable impact on key results.",
                    "To change the subject of a meeting suddenly.",
                    "To buy new office supplies."
                ),
                correctAnswer = 1,
                explanation = "'Move the needle' is standard jargon for driving a major, noticeable improvement in metrics."
            )
        )
    )
}
