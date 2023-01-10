#pragma once
#include <vector>
#include <iostream>

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

class Curve
{
public:
	Curve();
	~Curve();

	void init();
	void calculate_curve();

public:
	float tau = 0.5; // Coefficient for catmull-rom spline
	int num_points_per_segment = 200;

	// coefficient matrix M^T because openGL is column major
	float catrom_const[16] = {
		-tau, 2 - tau, tau - 2, tau,
		2 * tau, tau - 3, 3 - (2 * tau), -tau,
		-tau, 0, tau, 0,
		0, 1, 0, 0
	};
	glm::mat4 m = glm::make_mat4(catrom_const);

	std::vector<glm::vec3> control_points_pos;
	std::vector<glm::vec3> curve_points_pos;
};