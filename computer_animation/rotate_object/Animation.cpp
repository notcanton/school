#include "Animation.h"

Animation::Animation()
{
	this->m_model_mat = glm::mat4(1.0f);
	a = 45;
	b = 45;
}

Animation::~Animation()
{
}

void Animation::init()
{
	reset();
}

void Animation::update(float delta_time)
{
}

// Assigment 1 functions:
void Animation::reset()
{
	m_model_mat = glm::mat4(1.0f);
	m_model_mat = glm::translate(m_model_mat, glm::vec3(5.0f, 0.0f, 0.0f));
}

void Animation::rotateX()
{
	m_model_mat = glm::rotate(m_model_mat, glm::radians(a), glm::vec3(1.0, 0.0, 0.0));
}

void Animation::rotateY()
{
	glm::mat4 temp = glm::mat4(1.0f);
	temp = glm::rotate(temp, glm::radians(b), glm::vec3(0.0, 1.0, 0.0));
	m_model_mat = temp * m_model_mat;
}
