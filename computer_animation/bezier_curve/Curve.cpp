#include "Curve.h"
#include "glm/ext.hpp" 

Curve::Curve()
{
}

Curve::~Curve()
{
}

void Curve::init()
{
	this->control_points_pos = {
		{ 0.0, 8.5, -2.0 },
		{ -3.0, 11.0, 2.3 },
		{ -6.0, 8.5, -2.5 },
		{ -4.0, 5.5, 2.8 },
		{ 1.0, 2.0, -4.0 },
		{ 4.0, 2.0, 3.0 },
		{ 7.0, 8.0, -2.0 },
		{ 3.0, 10.0, 3.7 }
	};
}

void Curve::calculate_curve()
{
	std::vector<glm::vec4> blend_funcs;
	for (int i = 1; i <= num_points_per_segment; i++) {
		// computes u
		float val = i / (float) num_points_per_segment;

		// computes U = [u^3, u^2, u, 1]
		glm::vec4 param = { std::pow(val, 3), std::pow(val, 2), std::pow(val, 1), 1 };

		// Computes F = U^T M => M^T U
		glm::vec4 coef = m * param;

		blend_funcs.push_back(coef);
	}

	for (int i = 0; i < control_points_pos.size(); i++) {
		// B^T = matrix where each column is {p_i-1, p_i, p_i+1, p_i+2}
		glm::mat4x3 segment = { control_points_pos[i - 1 > 0 ? i - 1 : control_points_pos.size() - 1],
								control_points_pos[i],
								control_points_pos[i + 1 < control_points_pos.size() ? i + 1 : i + 1 - control_points_pos.size()],
								control_points_pos[i + 2 < control_points_pos.size() ? i + 2 : i + 2 - control_points_pos.size()] };
	
		for (glm::vec4 coef : blend_funcs) {
			// computes curve point p = F^T B = B^T F
			curve_points_pos.push_back(segment * coef);
		}
		
	}

	
}
