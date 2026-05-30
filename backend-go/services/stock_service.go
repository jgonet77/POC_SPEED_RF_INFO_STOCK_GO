package services

import (
	"errors"
	"strings"

	"stock-api/models"
	"stock-api/repositories"
)

// StockService is the service layer for stock operations.
type StockService struct {
	Repo *repositories.StockRepository
}

// NewStockService creates a StockService backed by the given repository.
func NewStockService(repo *repositories.StockRepository) *StockService {
	return &StockService{Repo: repo}
}

// SearchStock validates the search criteria, queries the repository and maps
// the resulting rows to the response model.
//
// At least one of ArtCode, StkLieu or StkNosu must be a non-nil, non-empty
// string. A validation failure returns an error and an "error" response. A
// database failure returns the error together with an "error" response that
// carries an empty (non-nil) item slice.
func (s *StockService) SearchStock(req models.StockSearchRequest) (models.StockSearchResponse, error) {
	if !hasSearchCriterion(req) {
		err := errors.New("At least one of art_code, stk_lieu, or stk_nosu must be provided")
		return models.StockSearchResponse{
			Status:  "error",
			Message: err.Error(),
			Items:   []models.StockItem{},
		}, err
	}

	rows, err := s.Repo.SearchByActivity(req)
	if err != nil {
		return models.StockSearchResponse{
			Status:  "error",
			Message: err.Error(),
			Items:   []models.StockItem{},
		}, err
	}

	items := make([]models.StockItem, 0, len(rows))
	for _, row := range rows {
		items = append(items, models.StockItem{
			ArtCode: row.ArtCode,
			StkLieu: row.StkLieu,
			StkNosu: row.StkNosu,
			QuaCode: row.QuaCode,
			StkQte:  row.StkQte,
		})
	}

	return models.StockSearchResponse{
		Status: "success",
		Items:  items,
	}, nil
}

// hasSearchCriterion reports whether at least one optional search field is set
// to a non-empty value.
func hasSearchCriterion(req models.StockSearchRequest) bool {
	return isNonEmpty(req.ArtCode) || isNonEmpty(req.StkLieu) || isNonEmpty(req.StkNosu)
}

// isNonEmpty reports whether p is non-nil and not blank once trimmed.
func isNonEmpty(p *string) bool {
	return p != nil && strings.TrimSpace(*p) != ""
}
