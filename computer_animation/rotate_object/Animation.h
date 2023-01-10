#pragma once

#include <iostream>

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

class Animation
{
public:

	Animation();
	~Animation();

	float a;
	float b;

	void init();
	void update(float delta_time);

	// Assigment 1 functions:
	void reset();
	void rotateX();
	void rotateY();
	glm::mat4 get_model_mat() { return m_model_mat; };

private:
	glm::mat4 m_model_mat;
};

