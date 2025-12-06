document.addEventListener('turbo:load', function() {
    // FAQ 아코디언 기능
    const faqItems = document.querySelectorAll('.faq-item');
    
    faqItems.forEach(item => {
        const question = item.querySelector('.faq-question');
        
        question.addEventListener('click', () => {
            // 현재 클릭된 항목 이외의 모든 항목 닫기
            faqItems.forEach(otherItem => {
                if (otherItem !== item) {
                    otherItem.classList.remove('active');
                    otherItem.querySelector('.faq-answer').style.display = 'none';
                }
            });
            
            // 현재 항목 토글
            item.classList.toggle('active');
            const answer = item.querySelector('.faq-answer');
            
            if (item.classList.contains('active')) {
                answer.style.display = 'block';
            } else {
                answer.style.display = 'none';
            }
        });
    });
    
    // 이메일 복사 기능
    const emailElements = document.querySelectorAll('a[href^="mailto:"]');
    
    emailElements.forEach(element => {
        element.addEventListener('click', function(e) {
            e.preventDefault();
            
            const email = this.href.replace('mailto:', '');
            navigator.clipboard.writeText(email).then(() => {
                // 복사 성공 효과
                const originalText = this.textContent;
                this.textContent = '복사됨!';
                
                setTimeout(() => {
                    this.textContent = originalText;
                }, 2000);
            });
        });
    });
});