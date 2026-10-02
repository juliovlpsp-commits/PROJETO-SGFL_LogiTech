export default function getStyles(theme) {

    return {
        page: {
            height: '100dvh',
            minHeight: '100dvh',
            position: 'relative',
            overflow: 'hidden',
            backgroundColor: theme.bg,
            backgroundImage: theme.backgroundImage,
            backgroundSize: theme.backgroundSize || 'auto',
            color: theme.ink,
            fontFamily:
                "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
            padding: '20px 24px'
        },

        shell: {
            width: '100%',
            maxWidth: '1500px',
            height: '100%',
            minHeight: 0,
            margin: '0 auto',
            position: 'relative',
            zIndex: 2,
            display: 'flex',
            flexDirection: 'column'
        },

        header: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '20px',
            marginBottom: '16px',
            flex: '0 0 auto'
        },

        workspace: {
            position: 'relative',
            flex: '1 1 auto',
            minHeight: 0,
            overflowY: 'auto',
            overflowX: 'hidden',
            overscrollBehaviorY: 'contain',
            scrollBehavior: 'smooth',
            scrollbarGutter: 'stable',
            padding: '5px 7px 5px 0',
            border: `1px solid ${theme.border}`,
            borderRadius: '22px',
            backgroundColor: 'rgba(30, 13, 20, 0.25)',
            backgroundImage: 'linear-gradient(145deg, rgba(244, 233, 236, 0.035), rgba(165, 69, 82, 0.055))',
            backdropFilter: 'blur(20px) saturate(135%)',
            WebkitBackdropFilter: 'blur(20px) saturate(135%)',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.06)'
        },

        workspaceContent: {
            position: 'relative',
            minHeight: '100%',
            padding: '8px 10px 28px 7px'
        },

        workspaceTitleBar: {
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '16px',
            margin: '4px 4px 18px'
        },

        panelTransitionContent: {
            position: 'relative'
        },

        workspaceTitle: {
            margin: '-8px 0 0',
            fontFamily: "Georgia, 'Times New Roman', serif",
            fontSize: 'clamp(25px, 3vw, 38px)',
            lineHeight: 1.05,
            fontWeight: 400,
            letterSpacing: '-0.025em'
        },

        workspaceBackButton: {
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '38px',
            padding: '0 13px',
            borderRadius: '11px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surfaceAlt,
            color: theme.ink,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer',
            whiteSpace: 'nowrap'
        },

        deliveryCover: {
            position: 'relative',
            isolation: 'isolate',
            overflow: 'hidden',
            display: 'grid',
            gridTemplateColumns: 'minmax(0, 1.35fr) minmax(170px, 0.65fr)',
            alignItems: 'center',
            gap: '24px',
            minHeight: '275px',
            marginBottom: '22px',
            padding: 'clamp(24px, 4vw, 48px)',
            border: `1px solid ${theme.borderStrong}`,
            borderRadius: '22px',
            backgroundColor: 'rgba(30, 13, 20, 0.60)',
            backgroundImage: 'radial-gradient(circle at 83% 30%, rgba(165,69,82,0.23), transparent 33%), linear-gradient(135deg, rgba(244,233,236,0.055), rgba(30,13,20,0.20))',
            backdropFilter: 'blur(24px) saturate(145%)',
            WebkitBackdropFilter: 'blur(24px) saturate(145%)',
            boxShadow: '0 22px 54px rgba(0,0,0,0.18), inset 0 1px 0 rgba(255,255,255,0.08)'
        },

        deliveryCoverGate: {
            position: 'absolute',
            inset: '8px 10px 28px 7px',
            minHeight: 0,
            marginBottom: 0
        },

        deliveryCoverCopy: {
            position: 'relative',
            zIndex: 1,
            maxWidth: '720px'
        },

        deliveryCoverEyebrow: {
            display: 'block',
            marginBottom: '16px',
            color: theme.danger,
            fontSize: '10px',
            fontWeight: 800,
            letterSpacing: '0.16em'
        },

        deliveryCoverTitle: {
            margin: 0,
            maxWidth: '720px',
            fontFamily: "Georgia, 'Times New Roman', serif",
            fontSize: 'clamp(42px, 5.4vw, 78px)',
            lineHeight: 0.96,
            fontWeight: 400,
            letterSpacing: '-0.04em'
        },

        deliveryCoverText: {
            maxWidth: '550px',
            margin: '18px 0 22px',
            color: theme.inkSoft,
            fontSize: '13px',
            lineHeight: 1.7
        },

        deliveryCoverButton: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '12px',
            minHeight: '44px',
            padding: '0 16px',
            border: '1px solid rgba(200, 90, 110, 0.62)',
            borderRadius: '12px',
            backgroundImage: 'linear-gradient(135deg, #A54552 0%, #8D3F4B 52%, #6E202D 100%)',
            color: theme.accentInk,
            fontSize: '12px',
            fontWeight: 750,
            cursor: 'pointer',
            boxShadow: '0 12px 30px rgba(110,32,45,0.24), inset 0 1px 0 rgba(255,255,255,0.15)'
        },

        deliveryCoverArt: {
            position: 'relative',
            justifySelf: 'center',
            display: 'grid',
            placeItems: 'center',
            width: 'min(100%, 230px)',
            aspectRatio: '1',
            color: theme.inkSoft
        },

        deliveryCoverOrbit: {
            position: 'relative',
            display: 'grid',
            placeItems: 'center',
            width: '82%',
            height: '82%',
            border: '1px solid rgba(244,233,236,0.17)',
            borderRadius: '50%',
            backgroundImage: 'radial-gradient(circle, rgba(165,69,82,0.14), rgba(165,69,82,0.02) 63%, transparent 64%)',
            boxShadow: '0 0 60px rgba(165,69,82,0.12), inset 0 0 32px rgba(244,233,236,0.035)'
        },

        deliveryCoverPackage: {
            display: 'grid',
            placeItems: 'center',
            width: '82px',
            height: '82px',
            border: `1px solid ${theme.borderStrong}`,
            borderRadius: '26px',
            backgroundImage: 'linear-gradient(145deg, rgba(165,69,82,0.35), rgba(30,13,20,0.62))',
            backdropFilter: 'blur(18px)',
            WebkitBackdropFilter: 'blur(18px)',
            color: theme.accentInk,
            boxShadow: '0 16px 40px rgba(0,0,0,0.24)'
        },

        deliveryCoverNode: {
            position: 'absolute',
            top: '13%',
            right: '9%',
            width: '10px',
            height: '10px',
            borderRadius: '50%',
            backgroundColor: theme.danger,
            boxShadow: `0 0 0 6px ${theme.statuses.PENDENTE.bg}, 0 0 22px ${theme.danger}`
        },

        deliveryCoverCaption: {
            position: 'absolute',
            right: '2%',
            bottom: '2%',
            color: theme.inkSoft,
            fontSize: '9px',
            fontWeight: 700,
            letterSpacing: '0.15em'
        },

        scrollCue: {
            position: 'fixed',
            right: 'clamp(18px, 3vw, 38px)',
            bottom: 'clamp(18px, 3vh, 30px)',
            zIndex: 1500,
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            minHeight: '44px',
            padding: '0 15px',
            border: `1px solid ${theme.borderStrong}`,
            borderRadius: '999px',
            backgroundColor: 'rgba(39, 18, 27, 0.78)',
            backgroundImage: 'linear-gradient(135deg, rgba(165,69,82,0.32), rgba(244,233,236,0.055))',
            backdropFilter: 'blur(22px) saturate(165%)',
            WebkitBackdropFilter: 'blur(22px) saturate(165%)',
            color: theme.ink,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 12px 34px rgba(0,0,0,0.28), inset 0 1px 0 rgba(255,255,255,0.12)'
        },

        brand: {
            display: 'flex',
            alignItems: 'center',
            gap: '11px'
        },

        brandIcon: {
            width: '38px',
            height: '38px',
            borderRadius: '14px',
            backgroundImage: 'linear-gradient(135deg, #A54552 0%, #7A2D38 52%, #6E202D 100%)',
            backgroundColor: theme.accent,
            color: theme.accentInk,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
        },

        wordmark: {
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
            fontSize: '20px',
            lineHeight: 1.1
        },

        subtitle: {
            fontSize: '12px',
            color: theme.inkSoft
        },

        headerActions: {
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            flexWrap: 'wrap',
            justifyContent: 'flex-end'
        },

        btnResources: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '7px',
            padding: '9px 13px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor: theme.surface,
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.24), rgba(110, 32, 45, 0.10))',
            backdropFilter: 'blur(24px) saturate(165%)',
            WebkitBackdropFilter: 'blur(24px) saturate(165%)',
            color: theme.ink,
            fontSize: '12px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.10), 0 10px 30px rgba(0, 0, 0, 0.22)'
        },

        themeToggle: {
            width: '36px',
            height: '36px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor: theme.surface,
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.20), rgba(110, 32, 45, 0.08))',
            backdropFilter: 'blur(24px) saturate(165%)',
            WebkitBackdropFilter: 'blur(24px) saturate(165%)',
            color: theme.ink,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer'
        },

        btnLogout: {
            padding: '9px 13px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor: 'rgba(165, 69, 82, 0.10)',
            backgroundImage: 'linear-gradient(135deg, rgba(190, 70, 95, 0.20), rgba(110, 32, 45, 0.10))',
            backdropFilter: 'blur(24px) saturate(165%)',
            WebkitBackdropFilter: 'blur(24px) saturate(165%)',
            color: theme.danger,
            fontSize: '12px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.08), 0 10px 28px rgba(0, 0, 0, 0.20)'
        },

        alertaErro: {
            marginBottom: '16px',
            padding: '12px 14px',
            borderRadius: '9px',
            border:
                `1px solid ${theme.border}`,
            borderLeft:
                `4px solid ${theme.danger}`,
            backgroundColor:
            theme.statuses.PENDENTE.bg,
            color:
            theme.statuses.PENDENTE.ink,
            fontSize: '13px'
        },

        alertaSucesso: {
            marginBottom: '16px',
            padding: '12px 14px',
            borderRadius: '9px',
            border:
                `1px solid ${theme.border}`,
            borderLeft:
                `4px solid ${theme.statuses.ENTREGUE.dot}`,
            backgroundColor:
            theme.statuses.ENTREGUE.bg,
            color:
            theme.statuses.ENTREGUE.ink,
            fontSize: '13px'
        },

        summaryGrid: {
            display: 'grid',
            gridTemplateColumns:
                'repeat(3, minmax(0, 1fr))',
            gap: '12px',
            marginBottom: '18px'
        },

        grid: {
            display: 'grid',
            gridTemplateColumns:
                '330px minmax(0, 1fr)',
            gap: '18px',
            alignItems: 'start'
        },

        card: {
            backgroundColor:
            theme.surface,
            backgroundImage: 'linear-gradient(145deg, rgba(60, 22, 34, 0.42), rgba(20, 8, 12, 0.24))',
            backdropFilter: 'blur(22px) saturate(150%)',
            WebkitBackdropFilter: 'blur(22px) saturate(150%)',
            border:
                `1px solid ${theme.border}`,
            borderRadius: '18px',
            padding: '20px'
        },

        cardTitle: {
            margin: 0,
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
            fontSize: '18px',
            fontWeight: 400
        },

        form: {
            display: 'flex',
            flexDirection: 'column',
            gap: '14px'
        },

        label: {
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            color: theme.inkSoft,
            fontSize: '12px',
            fontWeight: 600
        },

        input: {
            width: '100%',
            boxSizing: 'border-box',
            padding: '10px 11px',
            borderRadius: '8px',
            border:
                `1px solid ${theme.borderStrong}`,
            backgroundColor:
            theme.surfaceAlt,
            color:
            theme.ink,
            fontSize: '14px'
        },

        inputWithSuffix: {
            display: 'flex',
            alignItems: 'stretch'
        },

        inputNumber: {
            flex: 1,
            minWidth: 0,
            boxSizing: 'border-box',
            padding: '10px 11px',
            borderRadius: '8px 0 0 8px',
            border:
                `1px solid ${theme.borderStrong}`,
            borderRight: 'none',
            backgroundColor:
            theme.surfaceAlt,
            color:
            theme.ink,
            fontSize: '14px'
        },

        suffix: {
            display: 'flex',
            alignItems: 'center',
            padding: '0 11px',
            borderRadius: '0 8px 8px 0',
            border:
                `1px solid ${theme.borderStrong}`,
            backgroundColor:
            theme.surfaceAlt,
            color:
            theme.inkSoft,
            fontSize: '13px'
        },

        statusInfo: {
            padding: '11px 12px',
            borderRadius: '8px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor:
            theme.surfaceAlt
        },

        statusInfoTop: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '10px',
            marginBottom: '6px',
            fontSize: '12px',
            fontWeight: 700
        },

        statusInfoText: {
            color: theme.inkSoft,
            fontSize: '11px',
            lineHeight: 1.45
        },

        btnSubmit: {
            width: '100%',
            padding: '11px',
            border: '1px solid rgba(200, 90, 110, 0.60)',
            borderRadius: '12px',
            backgroundColor: theme.accent,
            backgroundImage: 'linear-gradient(135deg, #A54552 0%, #8D3F4B 52%, #6E202D 100%)',
            backdropFilter: 'blur(26px) saturate(170%)',
            WebkitBackdropFilter: 'blur(26px) saturate(170%)',
            color: theme.accentInk,
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnDisabledLarge: {
            width: '100%',
            padding: '11px',
            border: 'none',
            borderRadius: '8px',
            backgroundColor: theme.borderStrong,
            color: theme.inkSoft,
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'not-allowed'
        },

        tableHeader: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '14px',
            marginBottom: '17px'
        },

        deliveryToolbar: {
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            flexWrap: 'wrap',
            gap: '8px',
            flex: '1 1 420px'
        },

        deliverySearch: {
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            flex: '1 1 250px',
            maxWidth: '320px',
            minWidth: '180px',
            height: '36px',
            boxSizing: 'border-box',
            padding: '0 10px',
            borderRadius: '10px',
            border: `1px solid ${theme.borderStrong}`,
            backgroundColor: theme.surfaceAlt,
            color: theme.inkSoft
        },

        deliverySearchInput: {
            flex: 1,
            minWidth: 0,
            width: '100%',
            height: '100%',
            padding: 0,
            border: 'none',
            outline: 'none',
            background: 'transparent',
            color: theme.ink,
            fontSize: '11px'
        },

        deliveryStatusFilter: {
            maxWidth: '100%',
            height: '36px',
            boxSizing: 'border-box',
            padding: '0 10px',
            borderRadius: '10px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surfaceAlt,
            color: theme.ink,
            fontSize: '11px',
            cursor: 'pointer'
        },

        tableSubtitle: {
            display: 'block',
            marginTop: '4px',
            color: theme.inkSoft,
            fontSize: '11px'
        },

        refreshButton: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '8px 10px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor:
            theme.surfaceAlt,
            color:
            theme.ink,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        tableWrap: {
            width: '100%'
        },

        table: {
            width: '100%',
            borderCollapse: 'collapse',
            minWidth: '850px'
        },

        th: {
            textAlign: 'left',
            padding: '9px 8px',
            borderBottom:
                `1px solid ${theme.border}`,
            color: theme.inkSoft,
            fontSize: '10px',
            textTransform: 'uppercase',
            letterSpacing: '0.04em'
        },

        tr: {
            borderBottom:
                `1px solid ${theme.border}`
        },

        td: {
            padding: '12px 8px',
            fontSize: '13px',
            verticalAlign: 'middle'
        },

        tdId: {
            padding: '12px 8px',
            fontSize: '13px',
            fontWeight: 700,
            verticalAlign: 'middle'
        },

        deliveryTitle: {
            fontWeight: 600
        },

        actions: {
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '5px'
        },

        btnPrimary: {
            padding: '7px 10px',
            border: '1px solid rgba(200, 90, 110, 0.60)',
            borderRadius: '10px',
            backgroundColor: theme.accent,
            backgroundImage: 'linear-gradient(135deg, rgba(178, 58, 84, 0.96), rgba(122, 31, 43, 0.92))',
            backdropFilter: 'blur(22px) saturate(170%)',
            WebkitBackdropFilter: 'blur(22px) saturate(170%)',
            color: theme.accentInk,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnCancel: {
            padding: '7px 10px',
            borderRadius: '10px',
            border:
                `1px solid rgba(165, 69, 82, 0.38)`,
            backgroundColor: 'rgba(165, 69, 82, 0.07)',
            backgroundImage: 'linear-gradient(135deg, rgba(190, 70, 95, 0.16), rgba(110, 32, 45, 0.08))',
            backdropFilter: 'blur(22px) saturate(170%)',
            WebkitBackdropFilter: 'blur(22px) saturate(170%)',
            color: theme.danger,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnFinalize: {
            padding: '7px 10px',
            border: '1px solid rgba(230, 189, 123, 0.55)',
            borderRadius: '10px',
            backgroundColor:
            theme.statuses.EM_TRANSITO.dot,
            backgroundImage: 'linear-gradient(135deg, rgba(176, 120, 48, 0.85), rgba(122, 76, 30, 0.88))',
            backdropFilter: 'blur(22px) saturate(170%)',
            WebkitBackdropFilter: 'blur(22px) saturate(170%)',
            color: '#FFFFFF',
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnDelete: {
            padding: '7px 10px',
            borderRadius: '10px',
            border:
                `1px solid rgba(165, 69, 82, 0.38)`,
            backgroundColor: 'rgba(165, 69, 82, 0.07)',
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.12), rgba(110, 32, 45, 0.10))',
            backdropFilter: 'blur(22px) saturate(170%)',
            WebkitBackdropFilter: 'blur(22px) saturate(170%)',
            color: theme.danger,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnDisabled: {
            padding: '7px 10px',
            borderRadius: '8px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor:
                'rgba(150, 110, 120, 0.14)',
            backgroundImage: 'linear-gradient(135deg, rgba(255,255,255,0.04), rgba(110, 50, 65, 0.10))',
            backdropFilter: 'blur(16px)',
            WebkitBackdropFilter: 'blur(16px)',
            color: theme.inkSoft,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'not-allowed'
        },

        emptyState: {
            minHeight: '270px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '7px',
            color: theme.inkSoft
        },

        pagination: {
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '12px',
            marginTop: '18px'
        },

        pageBtn: {
            padding: '8px 12px',
            borderRadius: '10px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor:
            theme.surfaceAlt,
            color: theme.ink,
            fontSize: '12px',
            fontWeight: 600,
            cursor: 'pointer'
        },

        pageInfo: {
            color: theme.inkSoft,
            fontSize: '12px'
        },

        modalOverlay: {
            position: 'fixed',
            inset: 0,
            backgroundColor:
                'rgba(0, 0, 0, 0.58)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 3000
        },

        modal: {
            width: '100%',
            maxWidth: '500px',
            backgroundColor:
            theme.surface,
            backgroundImage: 'linear-gradient(145deg, rgba(46, 18, 28, 0.90), rgba(20, 8, 12, 0.86))',
            backdropFilter: 'blur(28px) saturate(155%)',
            WebkitBackdropFilter: 'blur(28px) saturate(155%)',
            border:
                `1px solid ${theme.border}`,
            borderRadius: '14px',
            padding: '23px',
            boxShadow:
                '0 25px 60px rgba(0,0,0,0.25)'
        },

        modalHeader: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '15px',
            marginBottom: '20px'
        },

        modalTitle: {
            margin: 0,
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
            fontSize: '19px',
            fontWeight: 400
        },

        modalSubtitle: {
            margin: '6px 0 0',
            color: theme.inkSoft,
            fontSize: '12px'
        },

        modalClose: {
            width: '33px',
            height: '33px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.borderStrong}`,
            backgroundColor: 'rgba(46, 18, 28, 0.60)',
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.20), rgba(110, 32, 45, 0.10))',
            backdropFilter: 'blur(20px) saturate(170%)',
            WebkitBackdropFilter: 'blur(20px) saturate(170%)',
            color: theme.ink,
            fontSize: '21px',
            cursor: 'pointer'
        },

        modalActions: {
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '8px',
            marginTop: '5px'
        },

        btnSecondary: {
            padding: '9px 13px',
            borderRadius: '9px',
            border:
                `1px solid ${theme.borderStrong}`,
            backgroundColor: 'rgba(46, 18, 28, 0.58)',
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.16), rgba(110, 32, 45, 0.07))',
            backdropFilter: 'blur(20px) saturate(165%)',
            WebkitBackdropFilter: 'blur(20px) saturate(165%)',
            color: theme.ink,
            fontSize: '12px',
            fontWeight: 600,
            cursor: 'pointer'
        }
    };
}
