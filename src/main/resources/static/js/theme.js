class ThemeManager {
    constructor() {
        this.themeToggleBtn = document.getElementById('theme-toggle');
        this.theme = localStorage.getItem('theme') || 'light';
        
        this.initialize();
        this.setupEventListeners();
    }

    initialize() {
        if (this.theme === 'dark' || (!this.theme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            document.documentElement.setAttribute('data-theme', 'dark');
            this.theme = 'dark';
        } else {
            document.documentElement.setAttribute('data-theme', 'light');
            this.theme = 'light';
        }
        
        localStorage.setItem('theme', this.theme);
    }

    setupEventListeners() {
        this.themeToggleBtn?.addEventListener('click', () => this.toggleTheme());

        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
            if (!localStorage.getItem('theme')) {
                this.theme = e.matches ? 'dark' : 'light';
                this.applyTheme();
            }
        });
    }

    toggleTheme() {
        this.theme = this.theme === 'light' ? 'dark' : 'light';
        this.applyTheme();
    }

    applyTheme() {
        document.documentElement.setAttribute('data-theme', this.theme);
        localStorage.setItem('theme', this.theme);
    }
}

// 페이지 로드 시 테마 매니저 초기화
document.addEventListener('turbo:load', () => {
    new ThemeManager();
});