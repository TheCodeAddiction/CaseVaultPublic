import { requestJson, authHeader } from './client'

export function fetchDistrictOverview(token, { district } = {}) {
  const params = new URLSearchParams()
  if (district) params.set('district', district)

  const query = params.toString()
  return requestJson(`/api/reports/district-overview${query ? `?${query}` : ''}`, {
    headers: authHeader(token),
  })
}
