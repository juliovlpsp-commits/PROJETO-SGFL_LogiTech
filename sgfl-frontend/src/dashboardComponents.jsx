export function SummaryCard({
                         icon,
                         label,
                         value,
                         description,
                         theme
                     }) {
    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                gap: '13px',
                padding: '15px 16px',
                border:
                    `1px solid ${theme.border}`,
                borderRadius: '12px',
                backgroundColor: theme.surface
            }}
        >

            <div
                style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '10px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    backgroundColor:
                    theme.surfaceAlt,
                    color:
                    theme.accent
                }}
            >
                {icon}
            </div>

            <div>

                <div
                    style={{
                        fontSize: '12px',
                        color: theme.inkSoft,
                        fontWeight: 600
                    }}
                >
                    {label}
                </div>

                <div>
                    <strong
                        style={{
                            fontSize: '24px',
                            lineHeight: 1.1
                        }}
                    >
                        {value}
                    </strong>

                    <span
                        style={{
                            marginLeft: '5px',
                            color: theme.inkSoft,
                            fontSize: '11px'
                        }}
                    >
                        {description}
                    </span>
                </div>

            </div>

        </div>
    );
}

export function StatusBadge({
                         status,
                         theme
                     }) {

    const tone =
        theme.statuses[status] ||
        theme.statuses.PENDENTE;

    let label =
        status;

    if (status === 'PENDENTE') {
        label = 'Pendente';
    } else if (status === 'EM_TRANSITO') {
        label = 'Em trânsito';
    } else if (status === 'ENTREGUE') {
        label = 'Entregue';
    } else if (status === 'CANCELADA') {
        label = 'Cancelada';
    }

    return (
        <span
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '5px 10px',
                borderRadius: '999px',
                backgroundColor: tone.bg,
                color: tone.ink,
                fontSize: '11px',
                fontWeight: 700,
                whiteSpace: 'nowrap'
            }}
        >

            <span
                style={{
                    width: '6px',
                    height: '6px',
                    borderRadius: '50%',
                    backgroundColor: tone.dot
                }}
            />

            {label}

        </span>
    );
}

export function BoxIcon() {
    return (
        <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d="m3 7 9-4 9 4-9 4-9-4Z" />
            <path d="M3 7v10l9 4 9-4V7" />
            <path d="M12 11v10" />
        </svg>
    );
}

export function UserIcon() {
    return (
        <svg
            width="19"
            height="19"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <circle
                cx="12"
                cy="8"
                r="4"
            />

            <path
                d="M4 21c0-4 3.6-7 8-7s8 3 8 7"
            />
        </svg>
    );
}

export function TruckIcon() {
    return (
        <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d="M3 6h11v11H3z" />
            <path d="M14 9h4l3 3v5h-7z" />
            <circle
                cx="7"
                cy="19"
                r="2"
            />
            <circle
                cx="18"
                cy="19"
                r="2"
            />
        </svg>
    );
}

export function PackageIcon() {
    return (
        <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d="m4 7 8-4 8 4-8 4-8-4Z" />
            <path d="M4 7v10l8 4 8-4V7" />
            <path d="M12 11v10" />
        </svg>
    );
}

export function SettingsIcon() {
    return (
        <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <circle
                cx="12"
                cy="12"
                r="3"
            />

            <path
                d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-1.8 1.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-2.6V20a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1-1.8-1.8.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.6-1H6v-2.6h.2a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1 1.8-1.8.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.6V5h2.6v.2a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1 1.8 1.8-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v2.6h-.2a1.7 1.7 0 0 0-1.6 1Z"
            />
        </svg>
    );
}

export function RefreshIcon() {
    return (
        <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path
                d="M20 11a8.1 8.1 0 0 0-14.9-3L3 11"
            />

            <path
                d="M3 4v7h7"
            />

            <path
                d="M4 13a8.1 8.1 0 0 0 14.9 3L21 13"
            />

            <path
                d="M21 20v-7h-7"
            />
        </svg>
    );
}

export function SearchIcon() {
    return (
        <svg
            aria-hidden="true"
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
        >
            <circle cx="10.8" cy="10.8" r="6.8" />
            <path d="m16 16 4.5 4.5" />
        </svg>
    );
}

export function ScrollArrowIcon({ direction = 'down' }) {
    return (
        <svg
            aria-hidden="true"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d={direction === 'up' ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} />
        </svg>
    );
}

export function SunIcon() {
    return (
        <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
        >
            <circle
                cx="12"
                cy="12"
                r="4"
            />

            <path
                d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"
            />
        </svg>
    );
}

export function MoonIcon() {
    return (
        <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path
                d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z"
            />
        </svg>
    );
}
