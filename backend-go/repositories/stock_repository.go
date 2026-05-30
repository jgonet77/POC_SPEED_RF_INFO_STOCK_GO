package repositories

import (
	"database/sql"
	"fmt"
	"strconv"
	"strings"

	"stock-api/models"
)

// StockRow is a single stock entry returned by the stock repository.
type StockRow struct {
	ArtCode string
	StkLieu string
	StkNosu string
	QuaCode string
	StkQte  int
}

// StockRepository provides access to stock data (table STK_DAT).
type StockRepository struct {
	DB *sql.DB
}

// NewStockRepository wires the shared connection pool into the repository.
func NewStockRepository(db *sql.DB) *StockRepository {
	return &StockRepository{DB: db}
}

// SearchByActivity searches stock by article code, location and/or storage
// number within an activity. The activity filter is mandatory (AND); the other
// criteria are combined with OR. All values are passed as parameters using the
// go-mssqldb @pN positional placeholder style.
func (r *StockRepository) SearchByActivity(req models.StockSearchRequest) ([]StockRow, error) {
	// First parameter is always the activity filter (AND).
	params := []interface{}{req.ActCode}

	// Collect the optional OR criteria.
	var orCriteria []string
	addCriterion := func(column string, value *string) {
		if value == nil || *value == "" {
			return
		}
		// Next placeholder index is len(params)+1 (1-based).
		placeholder := "@p" + strconv.Itoa(len(params)+1)
		orCriteria = append(orCriteria, column+" LIKE "+placeholder)
		params = append(params, "%"+*value+"%")
	}

	addCriterion("ART_CODE", req.ArtCode)
	addCriterion("STK_LIEU", req.StkLieu)
	addCriterion("STK_NOSU", req.StkNosu)

	var query strings.Builder
	query.WriteString("SELECT ART_CODE, ISNULL(STK_LIEU,''), ISNULL(STK_NOSU,''), ISNULL(QUA_CODE,''), CAST(STK_QTE AS INT) ")
	query.WriteString("FROM STK_DAT ")
	query.WriteString("WHERE ACT_CODE = @p1")
	if len(orCriteria) > 0 {
		query.WriteString(" AND (")
		query.WriteString(strings.Join(orCriteria, " OR "))
		query.WriteString(")")
	}
	query.WriteString(" ORDER BY ART_CODE, STK_LIEU")

	rows, err := r.DB.Query(query.String(), params...)
	if err != nil {
		return nil, fmt.Errorf("stock search failed: %w", err)
	}
	defer rows.Close()

	var results []StockRow
	for rows.Next() {
		var item StockRow
		if err := rows.Scan(&item.ArtCode, &item.StkLieu, &item.StkNosu, &item.QuaCode, &item.StkQte); err != nil {
			return nil, fmt.Errorf("stock search scan failed: %w", err)
		}
		results = append(results, item)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("stock search iteration failed: %w", err)
	}

	return results, nil
}
