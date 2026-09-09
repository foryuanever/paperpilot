/**
 * Academic Journal & Conference Recognition Engine
 * Comprehensive taxonomy covering CAS (中科院分区), JCR (Q1-Q4), CCF (A/B/C),
 * Indexing (SCI, SSCI, EI, TOP), and Impact Factor (IF).
 */

const JOURNAL_DATABASE = [
  // --- ACS (American Chemical Society) ---
  {
    keys: ["acs sensors", "acs sens", "acs sens.", "acssensors"],
    name: "ACS Sensors",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 8.2",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 8.2"]
  },
  {
    keys: ["jacs", "journal of the american chemical society", "j. am. chem. soc.", "j am chem soc"],
    name: "Journal of the American Chemical Society",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 14.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 14.4"]
  },
  {
    keys: ["acs nano", "acsnano"],
    name: "ACS Nano",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 15.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 15.8"]
  },
  {
    keys: ["acs applied materials & interfaces", "acs applied materials and interfaces", "acs appl mater interfaces", "acs appl. mater. interfaces", "acsami"],
    name: "ACS Applied Materials & Interfaces",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 8.3",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "TOP", "SCI", "IF 8.3"]
  },
  {
    keys: ["chemical reviews", "chem rev", "chem. rev."],
    name: "Chemical Reviews",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 51.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 51.4"]
  },
  {
    keys: ["accounts of chemical research", "acc chem res", "acc. chem. res."],
    name: "Accounts of Chemical Research",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 16.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 16.4"]
  },
  {
    keys: ["acs catalysis", "acs catal", "acs catal."],
    name: "ACS Catalysis",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 11.3",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 11.3"]
  },
  {
    keys: ["acs energy letters", "acs energy lett", "acs energy lett."],
    name: "ACS Energy Letters",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 19.3",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 19.3"]
  },
  {
    keys: ["acs central science", "acs cent sci", "acs cent. sci."],
    name: "ACS Central Science",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 12.7",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 12.7"]
  },
  {
    keys: ["analytical chemistry", "anal chem", "anal. chem."],
    name: "Analytical Chemistry",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 6.7",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 6.7"]
  },
  {
    keys: ["acs photonics"],
    name: "ACS Photonics",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 6.5",
    indices: ["SCI"],
    tags: ["中科院2区", "JCR Q1", "SCI", "IF 6.5"]
  },
  {
    keys: ["acs synthetic biology", "acs synth biol", "acs synth. biol."],
    name: "ACS Synthetic Biology",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 4.7",
    indices: ["SCI"],
    tags: ["中科院2区", "JCR Q1", "SCI", "IF 4.7"]
  },
  {
    keys: ["environmental science & technology", "environmental science and technology", "environ sci technol", "environ. sci. technol.", "es&t", "est"],
    name: "Environmental Science & Technology",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 10.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 10.8"]
  },
  {
    keys: ["biomacromolecules", "biomacromol"],
    name: "Biomacromolecules",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 5.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "TOP", "SCI", "IF 5.5"]
  },
  {
    keys: ["macromolecules"],
    name: "Macromolecules",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 5.1",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "TOP", "SCI", "IF 5.1"]
  },
  {
    keys: ["organic letters", "org lett", "org. lett."],
    name: "Organic Letters",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 4.9",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 4.9"]
  },

  // --- Nature Portfolio ---
  {
    keys: ["nature"],
    name: "Nature",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 50.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 50.5"]
  },
  {
    keys: ["nature communications", "nat commun", "nat. commun.", "natcommun"],
    name: "Nature Communications",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 14.7",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 14.7"]
  },
  {
    keys: ["nature biotechnology", "nat biotechnol", "nat. biotechnol."],
    name: "Nature Biotechnology",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 33.1",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 33.1"]
  },
  {
    keys: ["nature medicine", "nat med", "nat. med."],
    name: "Nature Medicine",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 58.7",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 58.7"]
  },
  {
    keys: ["nature methods", "nat methods", "nat. methods"],
    name: "Nature Methods",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 36.1",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 36.1"]
  },
  {
    keys: ["nature materials", "nat mater", "nat. mater."],
    name: "Nature Materials",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 37.2",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 37.2"]
  },
  {
    keys: ["nature nanotechnology", "nat nanotechnol", "nat. nanotechnol."],
    name: "Nature Nanotechnology",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 38.3",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 38.3"]
  },
  {
    keys: ["nature chemistry", "nat chem", "nat. chem."],
    name: "Nature Chemistry",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 19.2",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 19.2"]
  },
  {
    keys: ["nature machine intelligence", "nat mach intell", "nat. mach. intell."],
    name: "Nature Machine Intelligence",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 18.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 18.8"]
  },
  {
    keys: ["nature biomedical engineering", "nat biomed eng", "nat. biomed. eng."],
    name: "Nature Biomedical Engineering",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 26.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 26.8"]
  },
  {
    keys: ["scientific reports", "sci rep", "sci. rep."],
    name: "Scientific Reports",
    cas: "中科院3区",
    jcr: "JCR Q1",
    if: "IF 3.8",
    indices: ["SCI"],
    tags: ["中科院3区", "JCR Q1", "SCI", "IF 3.8"]
  },

  // --- Science / AAAS ---
  {
    keys: ["science"],
    name: "Science",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 44.7",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 44.7"]
  },
  {
    keys: ["science advances", "sci adv", "sci. adv."],
    name: "Science Advances",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 11.7",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 11.7"]
  },
  {
    keys: ["science robotics", "sci robot", "sci. robot."],
    name: "Science Robotics",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 19.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 19.5"]
  },

  // --- Cell Press ---
  {
    keys: ["cell"],
    name: "Cell",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 45.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 45.5"]
  },
  {
    keys: ["cell reports", "cell rep", "cell. rep."],
    name: "Cell Reports",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 7.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 7.5"]
  },
  {
    keys: ["chem", "cell chem"],
    name: "Chem",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 19.1",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 19.1"]
  },
  {
    keys: ["joule"],
    name: "Joule",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 38.6",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 38.6"]
  },
  {
    keys: ["matter"],
    name: "Matter",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 17.3",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 17.3"]
  },
  {
    keys: ["iscience"],
    name: "iScience",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 4.6",
    indices: ["SCI"],
    tags: ["中科院2区", "JCR Q1", "SCI", "IF 4.6"]
  },

  // --- Wiley ---
  {
    keys: ["advanced materials", "adv mater", "adv. mater."],
    name: "Advanced Materials",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 27.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 27.4"]
  },
  {
    keys: ["angewandte chemie", "angewandte chemie international edition", "angew chem int ed", "angew. chem. int. ed.", "angewandte"],
    name: "Angewandte Chemie International Edition",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 16.1",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 16.1"]
  },
  {
    keys: ["advanced functional materials", "adv funct mater", "adv. funct. mater."],
    name: "Advanced Functional Materials",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 18.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 18.5"]
  },
  {
    keys: ["advanced energy materials", "adv energy mater", "adv. energy mater."],
    name: "Advanced Energy Materials",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 24.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 24.4"]
  },
  {
    keys: ["small", "small journal"],
    name: "Small",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 13.0",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "TOP", "SCI", "IF 13.0"]
  },
  {
    keys: ["advanced science", "adv sci", "adv. sci."],
    name: "Advanced Science",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 14.3",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 14.3"]
  },

  // --- Elsevier ---
  {
    keys: ["biosensors and bioelectronics", "biosens bioelectron", "biosens. bioelectron.", "biosensors & bioelectronics"],
    name: "Biosensors and Bioelectronics",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 10.7",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 10.7"]
  },
  {
    keys: ["sensors and actuators b: chemical", "sensors and actuators b chemical", "sens actuators b chem", "sens. actuators b chem.", "sensors and actuators b"],
    name: "Sensors and Actuators B: Chemical",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 8.0",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 8.0"]
  },
  {
    keys: ["talanta"],
    name: "Talanta",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 5.6",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 5.6"]
  },
  {
    keys: ["analytica chimica acta", "anal chim acta", "anal. chim. acta"],
    name: "Analytica Chimica Acta",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 5.7",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 5.7"]
  },
  {
    keys: ["chemical engineering journal", "chem eng j", "chem. eng. j.", "cej"],
    name: "Chemical Engineering Journal",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 13.3",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 13.3"]
  },
  {
    keys: ["journal of hazardous materials", "j hazard mater", "j. hazard. mater."],
    name: "Journal of Hazardous Materials",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 12.2",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 12.2"]
  },
  {
    keys: ["biomaterials"],
    name: "Biomaterials",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 12.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 12.8"]
  },
  {
    keys: ["nano energy"],
    name: "Nano Energy",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 16.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 16.8"]
  },
  {
    keys: ["carbon"],
    name: "Carbon",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 10.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "TOP", "SCI", "IF 10.5"]
  },
  {
    keys: ["the lancet", "lancet"],
    name: "The Lancet",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 98.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 98.4"]
  },
  {
    keys: ["pattern recognition", "pattern recogn", "pattern recogn."],
    name: "Pattern Recognition",
    cas: "中科院1区",
    jcr: "JCR Q1",
    ccf: "CCF B",
    if: "IF 7.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "CCF B", "SCI", "IF 7.5"]
  },
  {
    keys: ["artificial intelligence", "artif intell", "artif. intell."],
    name: "Artificial Intelligence",
    cas: "中科院2区",
    jcr: "JCR Q1",
    ccf: "CCF A",
    if: "IF 14.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "CCF A", "SCI", "IF 14.4"]
  },

  // --- IEEE Transactions ---
  {
    keys: ["ieee transactions on pattern analysis and machine intelligence", "ieee trans pattern anal mach intell", "tpami", "ieee tpami"],
    name: "IEEE TPAMI",
    cas: "中科院1区",
    jcr: "JCR Q1",
    ccf: "CCF A",
    if: "IF 20.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "CCF A", "SCI", "IF 20.8"]
  },
  {
    keys: ["ieee transactions on image processing", "ieee trans image process", "tip", "ieee tip"],
    name: "IEEE TIP",
    cas: "中科院1区",
    jcr: "JCR Q1",
    ccf: "CCF A",
    if: "IF 10.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "CCF A", "SCI", "IF 10.8"]
  },
  {
    keys: ["ieee transactions on neural networks and learning systems", "ieee trans neural netw learn syst", "tnnls", "ieee tnnls"],
    name: "IEEE TNNLS",
    cas: "中科院1区",
    jcr: "JCR Q1",
    ccf: "CCF B",
    if: "IF 10.2",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "CCF B", "SCI", "IF 10.2"]
  },
  {
    keys: ["ieee transactions on knowledge and data engineering", "ieee trans knowl data eng", "tkde", "ieee tkde"],
    name: "IEEE TKDE",
    cas: "中科院2区",
    jcr: "JCR Q1",
    ccf: "CCF A",
    if: "IF 8.9",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "TOP", "CCF A", "SCI", "IF 8.9"]
  },
  {
    keys: ["ieee transactions on software engineering", "ieee trans softw eng", "tse", "ieee tse"],
    name: "IEEE TSE",
    cas: "中科院1区",
    jcr: "JCR Q1",
    ccf: "CCF A",
    if: "IF 6.5",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "CCF A", "SCI", "IF 6.5"]
  },
  {
    keys: ["ieee transactions on medical imaging", "ieee trans med imaging", "tmi", "ieee tmi"],
    name: "IEEE TMI",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 8.9",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 8.9"]
  },
  {
    keys: ["ieee internet of things journal", "ieee iot j", "ieee iot"],
    name: "IEEE Internet of Things Journal",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 8.2",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 8.2"]
  },
  {
    keys: ["ieee sensors journal", "ieee sensors"],
    name: "IEEE Sensors Journal",
    cas: "中科院3区",
    jcr: "JCR Q1",
    if: "IF 4.3",
    indices: ["SCI", "EI"],
    tags: ["中科院3区", "JCR Q1", "SCI", "EI", "IF 4.3"]
  },

  // --- Computer Science Top Conferences ---
  {
    keys: ["cvpr", "ieee/cvf conference on computer vision and pattern recognition", "computer vision and pattern recognition"],
    name: "CVPR",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "计算机视觉"]
  },
  {
    keys: ["iccv", "ieee/cvf international conference on computer vision", "international conference on computer vision"],
    name: "ICCV",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "计算机视觉"]
  },
  {
    keys: ["eccv", "european conference on computer vision"],
    name: "ECCV",
    ccf: "CCF B",
    tags: ["CCF B", "顶级会议", "计算机视觉"]
  },
  {
    keys: ["neurips", "nips", "neural information processing systems", "advances in neural information processing systems"],
    name: "NeurIPS",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "人工智能"]
  },
  {
    keys: ["icml", "international conference on machine learning"],
    name: "ICML",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "机器学习"]
  },
  {
    keys: ["iclr", "international conference on learning representations"],
    name: "ICLR",
    ccf: "顶会",
    tags: ["顶会", "深度学习", "人工智能"]
  },
  {
    keys: ["acl", "annual meeting of the association for computational linguistics"],
    name: "ACL",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "自然语言处理"]
  },
  {
    keys: ["emnlp", "empirical methods in natural language processing"],
    name: "EMNLP",
    ccf: "CCF B",
    tags: ["CCF B", "顶级会议", "自然语言处理"]
  },
  {
    keys: ["naacl", "human language technologies: the annual conference of the north american chapter of the acl"],
    name: "NAACL",
    ccf: "CCF B",
    tags: ["CCF B", "顶级会议", "自然语言处理"]
  },
  {
    keys: ["aaai", "aaai conference on artificial intelligence"],
    name: "AAAI",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "人工智能"]
  },
  {
    keys: ["ijcai", "international joint conference on artificial intelligence"],
    name: "IJCAI",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "人工智能"]
  },
  {
    keys: ["kdd", "acm sigkdd", "knowledge discovery and data mining"],
    name: "KDD",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "数据挖掘"]
  },
  {
    keys: ["sigmod", "acm sigmod"],
    name: "SIGMOD",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "数据库"]
  },
  {
    keys: ["vldb", "very large data bases"],
    name: "VLDB",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "数据库"]
  },
  {
    keys: ["icse", "international conference on software engineering"],
    name: "ICSE",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "软件工程"]
  },
  {
    keys: ["chi", "acm chi", "human factors in computing systems"],
    name: "CHI",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "人机交互"]
  },
  {
    keys: ["siggraph", "acm siggraph"],
    name: "SIGGRAPH",
    ccf: "CCF A",
    tags: ["CCF A", "顶级会议", "计算机图形学"]
  },

  // --- Other High Impact Journals ---
  {
    keys: ["pnas", "proceedings of the national academy of sciences", "proc natl acad sci usa", "proc. natl. acad. sci. u.s.a."],
    name: "PNAS",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 9.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 9.4"]
  },
  {
    keys: ["nucleic acids research", "nucleic acids res", "nucleic acids res.", "nar"],
    name: "Nucleic Acids Research",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 16.6",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 16.6"]
  },
  {
    keys: ["bioinformatics"],
    name: "Bioinformatics",
    cas: "中科院2区",
    jcr: "JCR Q1",
    ccf: "CCF B",
    if: "IF 4.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "TOP", "CCF B", "SCI", "IF 4.4"]
  },
  {
    keys: ["briefings in bioinformatics", "brief bioinform", "brief. bioinform."],
    name: "Briefings in Bioinformatics",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 6.8",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 6.8"]
  },
  {
    keys: ["chemical science", "chem sci", "chem. sci."],
    name: "Chemical Science",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 7.6",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 7.6"]
  },
  {
    keys: ["chemical society reviews", "chem soc rev", "chem. soc. rev."],
    name: "Chemical Society Reviews",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 40.4",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 40.4"]
  },
  {
    keys: ["physical review letters", "phys rev lett", "phys. rev. lett.", "prl"],
    name: "Physical Review Letters",
    cas: "中科院1区",
    jcr: "JCR Q1",
    if: "IF 8.1",
    indices: ["SCI", "TOP"],
    tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 8.1"]
  },
  {
    keys: ["sensors"],
    name: "Sensors (MDPI)",
    cas: "中科院3区",
    jcr: "JCR Q2",
    if: "IF 3.4",
    indices: ["SCI", "EI"],
    tags: ["中科院3区", "JCR Q2", "SCI", "EI", "IF 3.4"]
  },
  {
    keys: ["molecules"],
    name: "Molecules (MDPI)",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 4.2",
    indices: ["SCI"],
    tags: ["中科院2区", "JCR Q1", "SCI", "IF 4.2"]
  },
  {
    keys: ["international journal of molecular sciences", "ijms"],
    name: "IJMS",
    cas: "中科院2区",
    jcr: "JCR Q1",
    if: "IF 4.9",
    indices: ["SCI", "TOP"],
    tags: ["中科院2区", "JCR Q1", "TOP", "SCI", "IF 4.9"]
  }
];

// Pre-compile sorted candidate entries by key length descending
const SORTED_JOURNAL_ENTRIES = JOURNAL_DATABASE.flatMap(item =>
  item.keys.map(key => ({
    rawKey: key,
    cleanKey: normalizeText(key),
    item
  }))
).sort((a, b) => b.cleanKey.length - a.cleanKey.length);

/**
 * Match journal metrics by source string, paper title, or source URL/DOI.
 * Returns { name, cas, jcr, if, ccf, indices, tags } or null.
 */
export function matchJournalMetrics(source = "", title = "", sourceUrl = "") {
  const normSource = normalizeText(source);
  const normTitle = normalizeText(title);
  const normUrl = normalizeText(sourceUrl);

  if (!normSource && !normTitle && !normUrl) return null;

  // 1. Exact or whole-phrase match in database (longest key first)
  for (const { cleanKey, item } of SORTED_JOURNAL_ENTRIES) {
    if (!cleanKey) continue;

    if (normSource) {
      if (normSource === cleanKey) return item;
      if (cleanKey.length <= 4) {
        const regex = new RegExp(`(?:^|\\s)${escapeRegExp(cleanKey)}(?:$|\\s)`);
        if (regex.test(normSource)) return item;
      } else {
        if (normSource.includes(cleanKey)) return item;
      }
    }

    if (normUrl && cleanKey.length >= 4) {
      const compactKey = cleanKey.replace(/\s+/g, "");
      if (normUrl.includes(compactKey)) return item;
    }
  }

  // 2. Intelligent pattern & publisher heuristics
  if (normSource || normTitle || normUrl) {
    const combined = `${normSource} ${normTitle} ${normUrl}`;

    if (/\bacs sens/i.test(combined) || /acssensors/i.test(combined)) {
      return {
        name: "ACS Sensors",
        cas: "中科院1区",
        jcr: "JCR Q1",
        if: "IF 8.2",
        indices: ["SCI", "TOP"],
        tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "IF 8.2"]
      };
    }

    if (/\b(nature\s+[a-z]+|nature)\b/i.test(normSource)) {
      return {
        name: normSource,
        cas: "中科院1区",
        jcr: "JCR Q1",
        indices: ["SCI", "TOP"],
        tags: ["中科院1区", "JCR Q1", "TOP", "SCI"]
      };
    }

    if (/\b(science\s+[a-z]+|science)\b/i.test(normSource)) {
      return {
        name: normSource,
        cas: "中科院1区",
        jcr: "JCR Q1",
        indices: ["SCI", "TOP"],
        tags: ["中科院1区", "JCR Q1", "TOP", "SCI"]
      };
    }

    if (/\b(cell\s+[a-z]+|cell)\b/i.test(normSource)) {
      return {
        name: normSource,
        cas: "中科院1区",
        jcr: "JCR Q1",
        indices: ["SCI", "TOP"],
        tags: ["中科院1区", "JCR Q1", "TOP", "SCI"]
      };
    }

    if (/^ieee\s+transactions\s+on\s+/i.test(normSource) || /^ieee\s+trans\b/i.test(normSource)) {
      return {
        name: normSource,
        cas: "中科院1区",
        jcr: "JCR Q1",
        indices: ["SCI", "TOP", "EI"],
        tags: ["中科院1区", "JCR Q1", "TOP", "SCI", "EI"]
      };
    }

    if (/^acs\s+[a-z]+/i.test(normSource)) {
      return {
        name: normSource,
        cas: "中科院1区",
        jcr: "JCR Q1",
        indices: ["SCI", "TOP"],
        tags: ["中科院1区", "JCR Q1", "TOP", "SCI"]
      };
    }

    if (/^journal\s+of\s+/i.test(normSource) || /\bchem\b|\bbio\b|\bphys\b|\bappl\b/i.test(normSource)) {
      if (normSource !== "个人文献" && normSource.length > 5) {
        return {
          name: normSource,
          cas: "中科院2区",
          jcr: "JCR Q1",
          indices: ["SCI"],
          tags: ["中科院2区", "JCR Q1", "SCI"]
        };
      }
    }
  }

  return null;
}

function normalizeText(text) {
  if (!text) return "";
  return String(text)
    .toLowerCase()
    .replace(/[.,:;\\/|()\[\]{}"'’`-]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function escapeRegExp(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
