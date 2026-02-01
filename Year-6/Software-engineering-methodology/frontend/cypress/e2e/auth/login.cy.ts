describe('Login Flow', () => {
  beforeEach(() => {
    cy.visit('/login');
  });

  it('should display login page with correct elements', () => {
    cy.contains('h2', 'ITMO PAI').should('be.visible');
    cy.contains('label', 'Username:').should('be.visible');
    cy.contains('label', 'Password:').should('be.visible');
    cy.get('input[type="text"]').should('be.visible');
    cy.get('input[type="password"]').should('be.visible');
    cy.contains('button', 'Login').should('be.visible');
  });

  it('should allow user to type in username and password fields', () => {
    cy.get('input[type="text"]').type('testuser').should('have.value', 'testuser');
    cy.get('input[type="password"]').type('password123').should('have.value', 'password123');
  });

  it('should submit login form when clicking Login button', () => {
    cy.get('input[type="text"]').type('testuser');
    cy.get('input[type="password"]').type('password123');
    cy.contains('button', 'Login').click();

    // Note: This will fail without a real backend
    // Add intercept for API calls in real tests
  });

  it('should handle login failure - 401 Unauthorized', () => {
    cy.intercept('POST', '**/login', {
      statusCode: 401,
      body: {
        message: 'Invalid credentials',
      },
    }).as('loginRequest');

    cy.get('input[type="text"]').type('wronguser');
    cy.get('input[type="password"]').type('wrongpass');
    cy.contains('button', 'Login').click();

    cy.wait('@loginRequest').then(() => {
      expect(cy.contains("Invalid username or password").should("be.visible"));
    });
  });

  it('should prevent form submission when pressing enter', () => {
    cy.get('input[type="text"]').type('testuser');
    cy.get('input[type="password"]').type('password123{enter}');

    // Form should not submit on enter
    cy.url().should('include', '/login');
  });

  it('should have empty fields on initial load', () => {
    cy.get('input[type="text"]').should('have.value', '');
    cy.get('input[type="password"]').should('have.value', '');
  });
});
