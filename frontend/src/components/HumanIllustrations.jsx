/**
 * Human and Editorial Vector Illustrations for Cogni-Care Landing Page.
 * Styled with Cogni-Care's warm, peaceful palette (teal/green, terracotta, sand, warm grey).
 * Designed for lightweight rendering, scalability, and emotional resonance.
 */

export function ElderlyGamerIllustration({ className = "", width = 280, height = 220 }) {
  return (
    <svg
      viewBox="0 0 280 220"
      width={width}
      height={height}
      className={`editorial-illustration ${className}`}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="Elderly adult enjoying a cognitive game on a tablet"
    >
      {/* Soft organic background circles */}
      <circle cx="140" cy="115" r="95" fill="#EAF3EE" />
      <circle cx="210" cy="70" r="32" fill="#FAEDE3" />
      <path
        d="M230 140 C245 125, 255 105, 248 85"
        stroke="#D98D57"
        strokeWidth="2"
        strokeLinecap="round"
        strokeDasharray="4 6"
      />

      {/* Gentle plant/leaves motif in background */}
      <path
        d="M55 170 Q45 130 70 110 Q78 135 65 170 Z"
        fill="#D4EADB"
      />
      <path
        d="M48 165 Q30 140 45 120 Q55 140 52 165 Z"
        fill="#B8DEC7"
      />

      {/* Body / Torso (Warm knit sweater) */}
      <path
        d="M95 210 C95 165, 115 150, 140 150 C165 150, 185 165, 185 210 Z"
        fill="#267362"
      />
      {/* Soft collar */}
      <path
        d="M125 150 C135 158, 145 158, 155 150 C150 162, 130 162, 125 150 Z"
        fill="#EAF3EE"
      />

      {/* Neck */}
      <rect x="133" y="132" width="14" height="22" rx="6" fill="#F0C8AF" />

      {/* Head */}
      <ellipse cx="140" cy="110" rx="22" ry="26" fill="#F0C8AF" />

      {/* Silver Hair */}
      <path
        d="M117 112 C114 90, 126 80, 140 80 C154 80, 166 90, 163 112 C160 102, 155 90, 140 90 C125 90, 120 102, 117 112 Z"
        fill="#A3B4AE"
      />
      <circle cx="140" cy="80" r="10" fill="#93A59E" />

      {/* Glasses */}
      <circle cx="132" cy="108" r="6.5" stroke="#163832" strokeWidth="1.8" fill="rgba(255,255,255,0.4)" />
      <circle cx="148" cy="108" r="6.5" stroke="#163832" strokeWidth="1.8" fill="rgba(255,255,255,0.4)" />
      <path d="M138.5 108 L141.5 108" stroke="#163832" strokeWidth="1.8" />
      <path d="M125.5 107 L120 105" stroke="#163832" strokeWidth="1.5" />
      <path d="M154.5 107 L160 105" stroke="#163832" strokeWidth="1.5" />

      {/* Facial features (Calm, smiling) */}
      <path d="M136 122 Q140 126 144 122" stroke="#B87C5E" strokeWidth="1.8" strokeLinecap="round" />
      <circle cx="127" cy="116" r="3" fill="#F5BFA6" opacity="0.6" />
      <circle cx="153" cy="116" r="3" fill="#F5BFA6" opacity="0.6" />

      {/* Arms holding tablet */}
      <path
        d="M102 185 C108 170, 120 178, 128 185"
        stroke="#267362"
        strokeWidth="14"
        strokeLinecap="round"
      />
      <path
        d="M178 185 C172 170, 160 178, 152 185"
        stroke="#267362"
        strokeWidth="14"
        strokeLinecap="round"
      />

      {/* Tablet Device */}
      <g transform="translate(110, 162)">
        <rect width="60" height="42" rx="6" fill="#173E36" />
        <rect x="3" y="3" width="54" height="36" rx="4" fill="#F9FCFA" />
        {/* Memory Game Cards Preview on tablet screen */}
        <rect x="8" y="8" width="10" height="12" rx="2" fill="#FAEDE3" stroke="#D98D57" strokeWidth="0.8" />
        <rect x="22" y="8" width="10" height="12" rx="2" fill="#FAEDE3" stroke="#D98D57" strokeWidth="0.8" />
        <rect x="36" y="8" width="10" height="12" rx="2" fill="#E2EFE9" />
        <rect x="8" y="24" width="10" height="11" rx="2" fill="#E2EFE9" />
        <rect x="22" y="24" width="10" height="11" rx="2" fill="#185A4E" />
        <rect x="36" y="24" width="10" height="11" rx="2" fill="#E2EFE9" />
      </g>

      {/* Hands holding the tablet */}
      <ellipse cx="112" cy="186" rx="5" ry="6" fill="#F0C8AF" />
      <ellipse cx="168" cy="186" rx="5" ry="6" fill="#F0C8AF" />

      {/* Floating Sparkle of Engagement */}
      <path
        d="M205 105 L208 97 L211 105 L219 108 L211 111 L208 119 L205 111 L197 108 Z"
        fill="#D98D57"
      />
    </svg>
  );
}

export function FamilyMomentIllustration({ className = "", width = 340, height = 240 }) {
  return (
    <svg
      viewBox="0 0 340 240"
      width={width}
      height={height}
      className={`editorial-illustration ${className}`}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="Family caregiver and senior sharing a calm routine together"
    >
      {/* Warm ambient backdrop */}
      <rect x="15" y="15" width="310" height="210" rx="28" fill="#F7FBF9" />
      <ellipse cx="170" cy="130" rx="120" ry="75" fill="#EEF7F2" />
      <circle cx="85" cy="65" r="28" fill="#FDF3EB" />

      {/* Small Window / Warm light representation */}
      <rect x="35" y="35" width="46" height="60" rx="8" fill="#FFF" stroke="#E1ECE6" strokeWidth="1.5" />
      <line x1="58" y1="35" x2="58" y2="95" stroke="#E1ECE6" strokeWidth="1.5" />
      <line x1="35" y1="65" x2="81" y2="65" stroke="#E1ECE6" strokeWidth="1.5" />

      {/* Table surface */}
      <path d="M40 195 L300 195" stroke="#C9DDD5" strokeWidth="3" strokeLinecap="round" />
      <path d="M70 195 L65 225" stroke="#C9DDD5" strokeWidth="2.5" strokeLinecap="round" />
      <path d="M270 195 L275 225" stroke="#C9DDD5" strokeWidth="2.5" strokeLinecap="round" />

      {/* Left Character: Elderly Parent */}
      <g id="elderly-parent">
        {/* Body / Cardigan */}
        <path
          d="M85 195 C85 155, 105 140, 130 140 C145 140, 155 148, 160 165 C145 175, 140 195, 140 195 Z"
          fill="#3E7D6F"
        />
        {/* Head & Silver Hair */}
        <ellipse cx="125" cy="108" rx="17" ry="21" fill="#EFCEBC" />
        <path
          d="M108 108 C105 90, 116 82, 128 82 C140 82, 146 90, 144 105 C140 96, 134 88, 124 88 C115 88, 111 96, 108 108 Z"
          fill="#9CAFA9"
        />
        <circle cx="127" cy="80" r="7" fill="#8DA09A" />
        {/* Glasses & Gentle Smile */}
        <circle cx="123" cy="106" r="4.5" stroke="#163832" strokeWidth="1.2" />
        <circle cx="134" cy="106" r="4.5" stroke="#163832" strokeWidth="1.2" />
        <line x1="127.5" y1="106" x2="129.5" y2="106" stroke="#163832" strokeWidth="1.2" />
        <path d="M124 117 Q128 120 132 117" stroke="#9E6E57" strokeWidth="1.4" strokeLinecap="round" />
      </g>

      {/* Center Device on table */}
      <g transform="translate(145, 168)">
        <rect width="44" height="28" rx="4" fill="#1C3F38" />
        <rect x="2.5" y="2" width="39" height="24" rx="3" fill="#FFF" />
        {/* Photo Prompt */}
        <circle cx="15" cy="13" r="5" fill="#D98D57" />
        <rect x="24" y="9" width="13" height="3" rx="1.5" fill="#3E7D6F" />
        <rect x="24" y="15" width="9" height="3" rx="1.5" fill="#A8C9BE" />
      </g>

      {/* Right Character: Adult Child / Family Caregiver */}
      <g id="family-caregiver">
        {/* Leaning in gently */}
        <path
          d="M195 195 C195 160, 205 142, 230 142 C255 142, 270 160, 270 195 Z"
          fill="#D98D57"
        />
        {/* Head & Dark Chestnut Hair */}
        <ellipse cx="218" cy="112" rx="16" ry="20" fill="#ECC8B6" />
        <path
          d="M202 115 C200 90, 212 84, 226 84 C240 84, 245 95, 242 120 C238 108, 230 92, 220 92 C210 92, 205 104, 202 115 Z"
          fill="#352620"
        />
        {/* Soft expression */}
        <ellipse cx="213" cy="110" rx="1.5" ry="2" fill="#2D211B" />
        <ellipse cx="223" cy="110" rx="1.5" ry="2" fill="#2D211B" />
        <path d="M214 120 Q218 123 222 120" stroke="#9A624E" strokeWidth="1.4" strokeLinecap="round" />
        {/* Arm reaching toward table */}
        <path
          d="M208 160 C195 168, 185 178, 175 186"
          stroke="#D98D57"
          strokeWidth="10"
          strokeLinecap="round"
        />
      </g>

      {/* Warm cup of tea on table */}
      <g transform="translate(112, 178)">
        <rect x="0" y="5" width="14" height="12" rx="3" fill="#FFF" stroke="#C9DDD5" strokeWidth="1.2" />
        <path d="M14 8 C17 8, 17 14, 14 14" stroke="#C9DDD5" strokeWidth="1.2" />
        {/* Gentle steam lines */}
        <path d="M5 2 Q3 0 5 -3" stroke="#D98D57" strokeWidth="1" strokeLinecap="round" opacity="0.6" />
        <path d="M9 2 Q11 0 9 -3" stroke="#D98D57" strokeWidth="1" strokeLinecap="round" opacity="0.6" />
      </g>

      {/* Floating Warm Heart */}
      <path
        d="M255 72 C255 65, 245 65, 245 74 C245 81, 255 86, 255 86 C255 86, 265 81, 265 74 C265 65, 255 65, 255 72 Z"
        fill="#E89F70"
      />
    </svg>
  );
}

export function CareTeamCircleIllustration({ className = "", width = 320, height = 200 }) {
  return (
    <svg
      viewBox="0 0 320 200"
      width={width}
      height={height}
      className={`editorial-illustration ${className}`}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="Circle of care: patient, family caregiver, and supportive care team"
    >
      {/* Ambient background ribbon / connection loop */}
      <path
        d="M50 140 C50 60, 270 60, 270 140 C270 170, 50 170, 50 140 Z"
        stroke="#CBE4D8"
        strokeWidth="2"
        strokeDasharray="4 6"
        fill="#F5FAF7"
      />

      {/* Node 1: Family Member (Left) */}
      <g transform="translate(45, 60)">
        <circle cx="30" cy="30" r="28" fill="#FAEDE3" stroke="#E6C8B2" strokeWidth="1.5" />
        {/* Character: Family Caregiver */}
        <path d="M16 52 C16 42, 22 38, 30 38 C38 38, 44 42, 44 52 Z" fill="#D98D57" />
        <circle cx="30" cy="27" r="10" fill="#EDC6B1" />
        <path d="M22 25 C22 17, 26 15, 30 15 C34 15, 38 17, 38 25 Z" fill="#3D291F" />
        <text x="30" y="72" textAnchor="middle" fill="#5F4633" fontSize="10" fontWeight="700" fontFamily="DM Sans">
          Family Caregiver
        </text>
      </g>

      {/* Node 2: Patient (Center, Prominent) */}
      <g transform="translate(125, 40)">
        <circle cx="35" cy="35" r="34" fill="#E4F2EC" stroke="#256E5D" strokeWidth="2" />
        {/* Character: Senior */}
        <path d="M18 62 C18 49, 26 44, 35 44 C44 44, 52 49, 52 62 Z" fill="#185A4E" />
        <circle cx="35" cy="32" r="12" fill="#F0C7AF" />
        <path d="M25 29 C25 20, 30 17, 35 17 C40 17, 45 20, 45 29 Z" fill="#9DAFA8" />
        <circle cx="35" cy="17" r="4.5" fill="#889C94" />
        {/* Glasses */}
        <circle cx="32" cy="31" r="3.2" stroke="#163832" strokeWidth="1" />
        <circle cx="38" cy="31" r="3.2" stroke="#163832" strokeWidth="1" />
        <text x="35" y="85" textAnchor="middle" fill="#185A4E" fontSize="11" fontWeight="800" fontFamily="Manrope">
          Senior Patient
        </text>
      </g>

      {/* Node 3: Care Professional / Clinician (Right) */}
      <g transform="translate(215, 60)">
        <circle cx="30" cy="30" r="28" fill="#EDF5F1" stroke="#BDDCD0" strokeWidth="1.5" />
        {/* Character: Clinician (Part of authorized team, soft teal jacket, stethoscope line) */}
        <path d="M16 52 C16 42, 22 38, 30 38 C38 38, 44 42, 44 52 Z" fill="#387A6C" />
        <circle cx="30" cy="27" r="10" fill="#E8C3AD" />
        <path d="M22 24 C22 16, 26 15, 30 15 C34 15, 38 16, 38 24 Z" fill="#2E4A42" />
        {/* Clean notepad/stethoscope motif */}
        <path d="M26 38 Q30 43 34 38" stroke="#FFF" strokeWidth="1" fill="none" />
        <text x="30" y="72" textAnchor="middle" fill="#386A5E" fontSize="10" fontWeight="700" fontFamily="DM Sans">
          Care Team Doctor
        </text>
      </g>

      {/* Sync Badge at bottom */}
      <g transform="translate(110, 162)">
        <rect width="100" height="24" rx="12" fill="#FFF" stroke="#C5DFD3" strokeWidth="1" />
        <circle cx="18" cy="12" r="4" fill="#2C7E6A" />
        <text x="56" y="15" textAnchor="middle" fill="#2A5C50" fontSize="10" fontWeight="700" fontFamily="DM Sans">
          Authorized Sync
        </text>
      </g>
    </svg>
  );
}

export function OfflineBufferIllustration({ className = "", width = 360, height = 110 }) {
  return (
    <svg
      viewBox="0 0 360 110"
      width={width}
      height={height}
      className={`editorial-illustration ${className}`}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="Offline Activity Buffer flow from device to caregiver web"
    >
      {/* Node 1: Mobile Device */}
      <g transform="translate(15, 18)">
        <rect width="48" height="74" rx="8" fill="#1C3F38" />
        <rect x="3" y="5" width="42" height="64" rx="5" fill="#FFF" />
        <circle cx="24" cy="25" r="8" fill="#FAEDE3" />
        <rect x="10" y="38" width="28" height="5" rx="2" fill="#185A4E" />
        <rect x="14" y="47" width="20" height="4" rx="2" fill="#CDE3D8" />
        <text x="24" y="85" textAnchor="middle" fill="#22473E" fontSize="9" fontWeight="700" fontFamily="DM Sans">
          Play on Device
        </text>
      </g>

      {/* Connector 1 */}
      <path d="M72 55 L108 55" stroke="#2D7A68" strokeWidth="2" strokeDasharray="3 3" />
      <polygon points="108,52 114,55 108,58" fill="#2D7A68" />

      {/* Node 2: Offline Activity Buffer (Center, Emphasized) */}
      <g transform="translate(120, 14)">
        <rect width="120" height="82" rx="14" fill="#FAEDE3" stroke="#E3BFA8" strokeWidth="1.5" />
        <rect x="10" y="10" width="100" height="24" rx="6" fill="#FFF" stroke="#ECC8B2" strokeWidth="1" />
        <circle cx="24" cy="22" r="5" fill="#D98D57" />
        <text x="64" y="25" textAnchor="middle" fill="#914D21" fontSize="9.5" fontWeight="800" fontFamily="DM Sans">
          Offline Buffer
        </text>
        <text x="60" y="52" textAnchor="middle" fill="#674B39" fontSize="9" fontWeight="600" fontFamily="DM Sans">
          Zero internet needed
        </text>
        <rect x="22" y="60" width="76" height="12" rx="6" fill="#E69968" />
        <text x="60" y="69" textAnchor="middle" fill="#FFF" fontSize="8" fontWeight="700" fontFamily="DM Sans">
          Safe on-device queue
        </text>
      </g>

      {/* Connector 2 */}
      <path d="M248 55 L282 55" stroke="#2D7A68" strokeWidth="2" strokeDasharray="3 3" />
      <polygon points="282,52 288,55 282,58" fill="#2D7A68" />

      {/* Node 3: Caregiver Portal Sync */}
      <g transform="translate(294, 20)">
        <rect width="52" height="42" rx="6" fill="#FFF" stroke="#B8DCD0" strokeWidth="1.5" />
        <rect x="0" y="0" width="52" height="12" rx="6" fill="#E4F2EC" />
        <line x1="8" y1="20" x2="32" y2="20" stroke="#185A4E" strokeWidth="2" strokeLinecap="round" />
        <line x1="8" y1="26" x2="44" y2="26" stroke="#C4DDD3" strokeWidth="2" strokeLinecap="round" />
        <line x1="8" y1="32" x2="24" y2="32" stroke="#D98D57" strokeWidth="2" strokeLinecap="round" />
        <path d="M12 42 L40 42 L46 56 L6 56 Z" fill="#E1EDE6" />
        <text x="26" y="80" textAnchor="middle" fill="#22473E" fontSize="9" fontWeight="700" fontFamily="DM Sans">
          Caregiver View
        </text>
      </g>
    </svg>
  );
}

export function ConnectedCareVisual({ className = "", width = 640, height = 480 }) {
  return (
    <svg
      viewBox="0 0 640 480"
      width={width}
      height={height}
      className={`editorial-illustration ${className}`}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-hidden="true"
    >
      {/* 1. Ambient Background Atmosphere */}
      <ellipse cx="320" cy="240" rx="300" ry="220" fill="#F4FAF6" opacity="0.8" />
      <circle cx="480" cy="180" r="140" fill="#FDF3EB" opacity="0.75" />
      <circle cx="160" cy="220" r="130" fill="#E8F4ED" opacity="0.75" />

      {/* 2. Soft Arched Window in Center Background */}
      <path
        d="M 320 290 L 320 150 A 95 95 0 0 1 510 150 L 510 290"
        stroke="#D5E8DF"
        strokeWidth="2.5"
        fill="#FAFCFB"
        opacity="0.85"
      />
      <path d="M 415 55 L 415 290" stroke="#D5E8DF" strokeWidth="1.8" />
      <path d="M 320 150 L 510 150" stroke="#D5E8DF" strokeWidth="1.8" />
      <path d="M 320 215 L 510 215" stroke="#D5E8DF" strokeWidth="1.8" />

      {/* 3. Framed Brain Art on Wall (Left) */}
      <rect x="36" y="68" width="86" height="106" rx="6" fill="#FFF" stroke="#E2EBE5" strokeWidth="2.5" />
      <rect x="42" y="74" width="74" height="94" rx="4" fill="#FBFDFC" />
      {/* Left hemisphere (mint) */}
      <path
        d="M 78 102 C 67 102, 59 110, 59 120 C 59 126, 63 130, 61 136 C 59 142, 63 150, 71 150 C 75 150, 78 146, 80 144 C 81 146, 82 147, 82 147 Z"
        fill="#B4DCB8"
      />
      {/* Right hemisphere (peach) */}
      <path
        d="M 82 102 C 93 102, 101 110, 101 120 C 101 126, 97 130, 99 136 C 101 142, 97 150, 89 150 C 85 150, 82 146, 80 144 C 79 146, 78 147, 78 147 Z"
        fill="#F8CBB4"
      />
      {/* Tiny heart in frame */}
      <path
        d="M 98 152 C 98 152, 94 148, 94 145.5 C 94 143.5, 96.5 142.5, 98 144 C 99.5 142.5, 102 143.5, 102 145.5 C 102 148, 98 152, 98 152 Z"
        fill="#D98D57"
      />

      {/* 4. Potted Plant on Table Left */}
      <path d="M 28 350 L 35 390 L 75 390 L 82 350 Z" fill="#FFF" stroke="#DDE7E1" strokeWidth="2" />
      <path d="M 55 350 Q 25 280 44 235 Q 66 280 55 350 Z" fill="#588C7D" />
      <path d="M 55 350 Q 82 285 90 255 Q 76 305 55 350 Z" fill="#72A595" />
      <path d="M 55 350 Q 12 305 16 270 Q 36 305 55 350 Z" fill="#9BC3B5" />

      {/* 5. Cozy Sofa / Chair Backing behind Figures */}
      <path
        d="M 85 390 C 85 320, 110 300, 200 300 C 350 300, 480 320, 480 390 Z"
        fill="#F5EDE4"
        opacity="0.85"
      />

      {/* 6. CAREGIVER (Daughter / Adult Caregiver leaning in with arm around senior) */}
      <g id="caregiver-figure">
        {/* Torso in Warm Terracotta Sweater */}
        <path
          d="M 265 400 C 265 295, 290 270, 335 270 C 380 270, 405 295, 405 400 Z"
          fill="#D68550"
        />
        {/* Soft collar */}
        <path
          d="M 322 270 C 328 278, 342 278, 348 270 C 344 282, 326 282, 322 270 Z"
          fill="#FDF3EA"
        />
        {/* Neck */}
        <rect x="327" y="250" width="16" height="22" rx="6" fill="#F7C5A8" />
        {/* Head */}
        <ellipse cx="335" cy="222" rx="27" ry="31" fill="#F7C5A8" />
        {/* Dark Chestnut Hair in Neat Top Bun */}
        <path
          d="M 307 224 C 303 190, 317 178, 337 178 C 357 178, 368 192, 364 228 C 356 212, 347 194, 333 194 C 319 194, 313 210, 307 224 Z"
          fill="#3C2A21"
        />
        <circle cx="340" cy="165" r="16" fill="#3C2A21" />
        <ellipse cx="340" cy="177" rx="9" ry="4" fill="#2B1D16" />
        {/* Empathetic Brows & Smiling Eyes */}
        <path d="M 322 210 Q 328 207 333 211" stroke="#3C2A21" strokeWidth="1.6" strokeLinecap="round" fill="none" />
        <path d="M 340 211 Q 345 207 351 210" stroke="#3C2A21" strokeWidth="1.6" strokeLinecap="round" fill="none" />
        <ellipse cx="327" cy="217" rx="2.2" ry="3.2" fill="#2B1D16" />
        <ellipse cx="346" cy="217" rx="2.2" ry="3.2" fill="#2B1D16" />
        {/* Warm Smile & Cheeks */}
        <path d="M 329 233 Q 336 240 343 233" stroke="#9E583A" strokeWidth="2.2" strokeLinecap="round" fill="none" />
        <circle cx="318" cy="227" r="4.5" fill="#F7BA9E" opacity="0.65" />
        <circle cx="355" cy="227" r="4.5" fill="#F7BA9E" opacity="0.65" />
        {/* Arm Draped Affectionately Around Grandmother's Shoulder */}
        <path
          d="M 285 300 C 255 290, 225 305, 205 324"
          stroke="#D68550"
          strokeWidth="16"
          strokeLinecap="round"
        />
        <ellipse cx="202" cy="326" rx="8" ry="7" fill="#F7C5A8" />
        {/* Left Arm Resting Forward on Table */}
        <path
          d="M 370 335 C 380 360, 380 380, 365 390"
          stroke="#D68550"
          strokeWidth="14"
          strokeLinecap="round"
        />
        <ellipse cx="363" cy="390" rx="7" ry="6" fill="#F7C5A8" />
      </g>

      {/* 7. GRANDMOTHER (Senior Adult in Forest Green Sweater) */}
      <g id="senior-grandmother">
        {/* Torso in Deep Forest Green Sweater */}
        <path
          d="M 120 400 C 120 305, 145 280, 185 280 C 225 280, 250 305, 250 400 Z"
          fill="#185A4E"
        />
        {/* Soft Inner White Collar */}
        <path
          d="M 172 280 C 178 288, 192 288, 198 280 C 194 293, 176 293, 172 280 Z"
          fill="#F4FAF7"
        />
        {/* Neck */}
        <rect x="177" y="260" width="16" height="22" rx="6" fill="#FEDAC7" />
        {/* Head */}
        <ellipse cx="185" cy="232" rx="28" ry="32" fill="#FEDAC7" />
        {/* Chic Silver/Lavender Bob Haircut with Bangs */}
        <path
          d="M 152 236 C 148 204, 165 188, 185 188 C 205 188, 222 204, 218 236 C 212 224, 204 204, 185 204 C 166 204, 158 224, 152 236 Z"
          fill="#A8B8BE"
        />
        <circle cx="157" cy="230" r="14" fill="#A8B8BE" />
        <circle cx="213" cy="230" r="14" fill="#A8B8BE" />
        {/* Round Glasses */}
        <circle cx="174" cy="230" r="9.5" stroke="#162E27" strokeWidth="2.2" fill="rgba(255,255,255,0.4)" />
        <circle cx="196" cy="230" r="9.5" stroke="#162E27" strokeWidth="2.2" fill="rgba(255,255,255,0.4)" />
        <path d="M 183.5 230 L 186.5 230" stroke="#162E27" strokeWidth="2" />
        {/* Smiling Crinkled Eyes behind Spectacles */}
        <path d="M 170 228 Q 174 225 178 228" stroke="#162E27" strokeWidth="1.8" strokeLinecap="round" fill="none" />
        <path d="M 192 228 Q 196 225 200 228" stroke="#162E27" strokeWidth="1.8" strokeLinecap="round" fill="none" />
        {/* Cheeks & Joyful Smile */}
        <circle cx="167" cy="240" r="4.5" fill="#F7BA9E" opacity="0.7" />
        <circle cx="203" cy="240" r="4.5" fill="#F7BA9E" opacity="0.7" />
        <path d="M 178 247 Q 185 254 192 247" stroke="#B26E4F" strokeWidth="2.2" strokeLinecap="round" fill="none" />
        {/* Right Arm Reaching Forward to Tap Tablet */}
        <path
          d="M 210 325 C 228 335, 248 345, 270 343"
          stroke="#185A4E"
          strokeWidth="16"
          strokeLinecap="round"
        />
        <circle cx="272" cy="342" r="8" fill="#FEDAC7" />
        <path d="M 275 342 L 286 337" stroke="#FEDAC7" strokeWidth="5" strokeLinecap="round" />
      </g>

      {/* 8. COGNITIVE TABLET (Propped up facing forward with Heart Motif) */}
      <g id="cognitive-tablet">
        {/* Tablet stand glow */}
        <ellipse cx="308" cy="384" rx="42" ry="12" fill="#D2E8DC" opacity="0.7" />
        {/* Forest Green Tablet Frame */}
        <rect
          x="254"
          y="302"
          width="112"
          height="86"
          rx="12"
          fill="#185A4E"
          stroke="#12463D"
          strokeWidth="2.5"
          transform="rotate(-2 254 302)"
        />
        {/* Tablet Screen */}
        <rect
          x="261"
          y="308"
          width="98"
          height="74"
          rx="8"
          fill="#1E6557"
          transform="rotate(-2 254 302)"
        />
        {/* Warm Golden-Peach Heart Motif on Tablet */}
        <path
          d="M 308 348 C 308 348, 298 339, 298 332 C 298 326, 304 324, 308 328 C 312 324, 318 326, 318 332 C 318 339, 308 348, 308 348 Z"
          fill="#E5A066"
        />
        {/* Touch Feedback Ring where Grandmother taps */}
        <circle cx="290" cy="338" r="9" stroke="#FFF" strokeWidth="1.8" fill="none" opacity="0.85" />
      </g>

      {/* 9. TABLETOP (Natural Wood Surface with Books & Coffee Mug) */}
      <rect x="15" y="388" width="610" height="24" rx="4" fill="#F4ECE2" stroke="#E5DACD" strokeWidth="1.5" />
      <rect x="25" y="412" width="590" height="12" fill="#EADBCE" opacity="0.6" />

      {/* Stack of Books (Left) */}
      <rect x="35" y="374" width="75" height="14" rx="2" fill="#CBE3D6" stroke="#B2D3C1" strokeWidth="1.2" />
      <rect x="38" y="362" width="68" height="12" rx="2" fill="#FCE9DC" stroke="#EDCFBD" strokeWidth="1.2" />

      {/* White Ceramic Coffee Mug with Heart (Right) */}
      <rect x="390" y="350" width="36" height="38" rx="6" fill="#FFF" stroke="#E2EBE6" strokeWidth="1.5" />
      <path d="M 426 358 C 435 358, 435 376, 426 376" stroke="#E2EBE6" strokeWidth="2.5" fill="none" />
      <path
        d="M 408 369 C 408 369, 404 365, 404 362.5 C 404 360.5, 406.5 359.5, 408 361 C 409.5 359.5, 412 360.5, 412 362.5 C 412 365, 408 369, 408 369 Z"
        fill="#D98D57"
      />

      {/* 10. Foreground Plant Leaves (Bottom Left Corner) */}
      <path d="M -10 490 Q 20 420 80 390 Q 50 460 -10 490 Z" fill="#24584B" />
      <path d="M 0 490 Q 60 450 110 440 Q 60 480 0 490 Z" fill="#3D7A6B" />
      <path d="M -20 440 Q 20 380 70 370 Q 30 420 -20 440 Z" fill="#589584" />
    </svg>
  );
}
