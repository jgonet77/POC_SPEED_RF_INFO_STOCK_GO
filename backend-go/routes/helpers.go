package routes

// ptr returns a pointer to the given value. It is a small generic helper used
// to populate the optional pointer fields of the response models.
func ptr[T any](v T) *T {
	return &v
}
