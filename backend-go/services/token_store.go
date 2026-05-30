package services

import "sync"

// TokenStore is a thread-safe, in-memory store mapping active JWT tokens to the
// login they were issued for. It replaces the Python `active_tokens` dict and
// provides the revocation check used by the auth middleware: a token is only
// considered valid if it is present in this store.
type TokenStore struct {
	mu     sync.RWMutex
	tokens map[string]string
}

// NewTokenStore creates an empty TokenStore ready for concurrent use.
func NewTokenStore() *TokenStore {
	return &TokenStore{
		tokens: make(map[string]string),
	}
}

// Store is the exported singleton shared by routes and middleware.
var Store = NewTokenStore()

// Add registers a token for the given login. If the token already exists its
// login is overwritten.
func (ts *TokenStore) Add(token, login string) {
	ts.mu.Lock()
	defer ts.mu.Unlock()
	ts.tokens[token] = login
}

// Contains reports whether the token is currently active (not revoked).
func (ts *TokenStore) Contains(token string) bool {
	ts.mu.RLock()
	defer ts.mu.RUnlock()
	_, ok := ts.tokens[token]
	return ok
}

// GetLogin returns the login associated with the token and whether it was found.
func (ts *TokenStore) GetLogin(token string) (string, bool) {
	ts.mu.RLock()
	defer ts.mu.RUnlock()
	login, ok := ts.tokens[token]
	return login, ok
}

// Remove revokes a token. It is a no-op if the token is not present.
func (ts *TokenStore) Remove(token string) {
	ts.mu.Lock()
	defer ts.mu.Unlock()
	delete(ts.tokens, token)
}
